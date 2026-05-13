package com.inkwell.notifiction;

import com.inkwell.notification.NotificationServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {NotificationServiceApplication.class, NotifictionApplicationTests.TestMailConfiguration.class})
class NotifictionApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestMailConfiguration {
		@Bean
		JavaMailSender javaMailSender() {
			return mock(JavaMailSender.class);
		}
	}

}
