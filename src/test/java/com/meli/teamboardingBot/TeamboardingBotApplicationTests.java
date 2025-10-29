package com.meli.teamboardingBot;

import com.meli.teamboardingBot.config.JdaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
	"discord.token=test-token-mock",
	"discord.guild.id=123456789"
})
@ActiveProfiles("test")
class TeamboardingBotApplicationTests {

	@Test
	void contextLoads() {
		// This test will fail to connect to Discord, but that's expected in tests
		// The context should load successfully with mock properties
	}

}
