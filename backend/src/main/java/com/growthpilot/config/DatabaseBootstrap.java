package com.growthpilot.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean tablesExist = false;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE lower(table_name) = 'businesses'",
                    Integer.class
            );
            tablesExist = count != null && count > 0;
        } catch (Exception e) {
            tablesExist = false;
        }

        if (!tablesExist) {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        jdbcTemplate.execute(trimmed);
                    } catch (Exception ignored) {
                    }
                }
            }
            return;
        }

        String[] columnFixes = {
                "ALTER TABLE businesses ADD COLUMN IF NOT EXISTS description TEXT",
                "ALTER TABLE customers ADD COLUMN IF NOT EXISTS phone VARCHAR(50)",
                "ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_id VARCHAR(64)",
                "ALTER TABLE customers ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)",
                "UPDATE customers SET customer_id = 'CUS-' || upper(substr(md5(random()::text), 1, 16)) WHERE customer_id IS NULL",
                "ALTER TABLE customers ADD COLUMN IF NOT EXISTS preferred_category VARCHAR(255)",
                "ALTER TABLE products ADD COLUMN IF NOT EXISTS description TEXT",
                "ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(255)",
                "ALTER TABLE products ADD COLUMN IF NOT EXISTS sku VARCHAR(128)",
                "ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000)",
                "ALTER TABLE products ADD COLUMN IF NOT EXISTS metadata TEXT",
                "UPDATE products SET sku = 'TM-' || lpad(id::text, 3, '0') WHERE sku IS NULL",
                "UPDATE products SET brand = split_part(name, ' ', 1) WHERE brand IS NULL",
                "UPDATE products SET description = name || ' selected by TechMart for quality, value and everyday usefulness.' WHERE description IS NULL",
                "UPDATE products SET image_url = CASE WHEN lower(category) = 'footwear' THEN 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=85' WHEN lower(category) = 'clothing' THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=85' WHEN lower(category) = 'beauty' THEN 'https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&w=900&q=85' WHEN lower(category) = 'accessories' THEN 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=900&q=85' WHEN lower(category) = 'appliances' THEN 'https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&w=900&q=85' WHEN lower(category) = 'stationery' THEN 'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=900&q=85' ELSE 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=85' END WHERE image_url IS NULL",
                "ALTER TABLE carts ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE orders ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE customer_events ADD COLUMN IF NOT EXISTS session_id VARCHAR(128)",
                "CREATE TABLE IF NOT EXISTS wishlist_items (id SERIAL PRIMARY KEY, customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE, product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, CONSTRAINT uk_wishlist_customer_product UNIQUE (customer_id, product_id))",
                "CREATE INDEX IF NOT EXISTS idx_wishlist_customer ON wishlist_items(customer_id)",
                "CREATE INDEX IF NOT EXISTS idx_customer_id ON customers(customer_id)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uk_customers_customer_id ON customers(customer_id) WHERE customer_id IS NOT NULL"
        };

        for (String statement : columnFixes) {
            try {
                jdbcTemplate.execute(statement);
            } catch (Exception ignored) {
                // Some DBs may already have the schema in a later state; ignore non-fatal schema drift.
            }
        }
    }
}
