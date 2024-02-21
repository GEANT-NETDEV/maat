package org.geant.maat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MaatApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaatApplication.class, args);
	}

}
