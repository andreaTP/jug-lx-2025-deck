package com.example.springboot;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

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

	private WasmModule module;

	private void uploadImpl(byte[] wasmBytes) {
		module = Parser.parse(wasmBytes);
	}

	private int computeImpl(int op1, int op2) {
		if (module == null) {
			throw new RuntimeException("Wasm module not provided");
		}
		var instance = Instance.builder(module).build();
		var result = (int) instance.exports().function("operation").apply(op1, op2)[0];

		return result;
	}
}
