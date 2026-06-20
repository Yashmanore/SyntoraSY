package com.example.societyhub;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SocietyhubApplicationTests {

	@Test
	void contextLoads() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = "$2a$10$cNNjEYxcKsm7fxiprAagSu4Fy/WfwFT1PWqP6TDfJvXSoiM2l1CUu";
		String[] candidates = {
				"Yashm123@",
				"pgadmin4",
				"admin",
				"admin123",
				"yash",
				"yash123",
				"yashmanore",
				"syntora",
				"password"
		};
		for (String candidate : candidates) {
			if (encoder.matches(candidate, hash)) {
				System.out.println(">>> FOUND MATCHING PASSWORD: " + candidate + " <<<");
				return;
			}
		}
		System.out.println(">>> NO MATCHING PASSWORD FOUND <<<");
	}

}
