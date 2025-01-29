package com.example.springboot;

import com.dylibso.chicory.log.Logger;
import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.ExternalValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static java.nio.file.Files.copy;

@RestController
public class WasmController {

	@GetMapping("/compute")
	public String compute(@RequestParam("op1") int op1, @RequestParam("op2") int op2) {
		int result = computeImpl(op1, op2);
		return "Your result is " + result;
	}

	@PostMapping(value = "/upload",
			consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public String upload(HttpServletRequest httpServletRequest) {
		try (InputStream inputStream = httpServletRequest.getInputStream()
		) {
			uploadImpl(inputStream.readAllBytes());
			return "Wasm uploaded";
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final Logger logger = new SystemLogger();

	private Module WASM_INTERP_MODULE = Parser.parse(WasmController.class.getResourceAsStream("/wasm-interp"));
	private byte[] wasmModule;

	private void uploadImpl(byte[] wasmBytes) {
		wasmModule = wasmBytes;
	}

	private int computeImpl(int op1, int op2) {
		if (wasmModule == null) {
			throw new RuntimeException("you need to upload a wasm module first");
		}
		try (var stdout = new ByteArrayOutputStream();
			 var wasmIS = new ByteArrayInputStream(wasmModule);
			 FileSystem fs =
					 Jimfs.newFileSystem(
							 Configuration.unix().toBuilder().setAttributeViews("unix").build())) {
			var optsBuilder = WasiOptions.builder();

			Path folder = fs.getPath(".");
			Path inputPath = folder.resolve("tmp.wasm");
			copy(wasmIS, inputPath, StandardCopyOption.REPLACE_EXISTING);
			optsBuilder.withDirectory(folder.toString(), folder);

			// example:
			// wasm-interp demo.wasm -r "operation" -a "i32:8" -a "i32:5"
			optsBuilder.withArguments(List.of(
					"wasm-interp",
					"tmp.wasm",
					"-r",
					"operation",
					"-a",
					"i32:" + op1,
					"-a",
					"i32:" + op2
			));
			optsBuilder.withStdout(stdout);

			var wasi = new WasiPreview1(this.logger, optsBuilder.build());
			var imports = new ExternalValues(wasi.toHostFunctions());

			Instance
					.builder(WASM_INTERP_MODULE)
					.withExternalValues(imports)
					.build();
			var result = new String(stdout.toByteArray());
			// example output:
			// operation(i32:8, i32:5) => i32:13
			var lastCol = result.lastIndexOf(':');
			return Integer.valueOf(result.substring(lastCol + 1).trim());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
