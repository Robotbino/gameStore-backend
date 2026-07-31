package com.gameStore.Bino;

import com.gameStore.Bino.models.Role;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.repositories.UserRepository;
import com.gameStore.Bino.service.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BinoApplication {

	private static final Logger log = LoggerFactory.getLogger(BinoApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BinoApplication.class, args);
	}

	// Seeds a demo USER account on first run so the admin-vs-user split is
	// immediately demoable. Idempotent — skips if the email already exists.
	@Bean
	CommandLineRunner seedDemoUser(UserRepository userRepository, UsersService usersService) {
		return args -> {
			final String demoEmail = "user@gamestore.com";
			final String demoPassword = "12345678";
			if (!userRepository.existsByEmail(demoEmail)) {
				usersService.addUser(Users.builder()
						.userName("DemoUser")
						.email(demoEmail)
						.password(demoPassword)
						.role(Role.USER)
						.build());
				log.info("Seeded demo USER — email={} password={}", demoEmail, demoPassword);
			}
			log.info("Demo accounts ready:");
			log.info("  ADMIN  email=admin@gamestore.com password=12345678");
			log.info("  USER   email={} password={}", demoEmail, demoPassword);
		};
	}

}
