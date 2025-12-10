package com.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@EnableScheduling
@SpringBootApplication
public class MarketplaceAuctionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketplaceAuctionsApplication.class, args);
	}
}
