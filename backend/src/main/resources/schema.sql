CREATE TABLE IF NOT EXISTS businesses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    razorpay_key_id VARCHAR(255),
    razorpay_key_secret VARCHAR(255),
    razorpay_webhook_secret VARCHAR(255),
    last_sync_at TIMESTAMP,
    sync_status VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    customer_id VARCHAR(64) UNIQUE,
    password_hash VARCHAR(255),
    phone VARCHAR(50),
    total_orders INTEGER DEFAULT 0,
    total_spend NUMERIC(12, 2) DEFAULT 0.0,
    average_order_value NUMERIC(12, 2) DEFAULT 0.0,
    last_order_date TIMESTAMP,
    preferred_category VARCHAR(255),
    engagement_score INTEGER DEFAULT 0,
    purchase_intent_score INTEGER DEFAULT 0,
    churn_risk VARCHAR(50) DEFAULT 'LOW',
    segment VARCHAR(100),
    razorpay_customer_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    brand VARCHAR(255),
    sku VARCHAR(128) UNIQUE,
    image_url VARCHAR(1000),
    metadata TEXT,
    price NUMERIC(12, 2) NOT NULL,
    stock INTEGER DEFAULT 0,
    total_sales INTEGER DEFAULT 0,
    views INTEGER DEFAULT 0,
    cart_additions INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    customer_id INTEGER REFERENCES customers(id),
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    razorpay_order_id VARCHAR(255),
    currency VARCHAR(10),
    receipt VARCHAR(255),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    price_at_purchase NUMERIC(12, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS carts (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    customer_id INTEGER REFERENCES customers(id),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    id SERIAL PRIMARY KEY,
    cart_id INTEGER REFERENCES carts(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_events (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    event_type VARCHAR(100) NOT NULL,
    product_id INTEGER REFERENCES products(id),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wishlist_items (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wishlist_customer_product UNIQUE (customer_id, product_id)
);

ALTER TABLE customer_events ADD COLUMN IF NOT EXISTS session_id VARCHAR(128);

CREATE TABLE IF NOT EXISTS recommendations (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES customers(id),
    product_id INTEGER REFERENCES products(id),
    score NUMERIC(5, 2) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS growth_opportunities (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    target_customer_id INTEGER REFERENCES customers(id),
    target_product_id INTEGER REFERENCES products(id),
    estimated_impact NUMERIC(12, 2),
    confidence_score INTEGER,
    recommended_action TEXT,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS campaigns (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    name VARCHAR(255) NOT NULL,
    target_segment VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    offer_details TEXT,
    generated_message TEXT,
    estimated_impact NUMERIC(12, 2),
    created_by_agent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_actions (
    id SERIAL PRIMARY KEY,
    business_id INTEGER REFERENCES businesses(id),
    user_id INTEGER REFERENCES users(id),
    goal TEXT,
    reasoning_summary TEXT,
    tool_used VARCHAR(255),
    parameters TEXT,
    result TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    business_id INTEGER REFERENCES businesses(id) NOT NULL,
    razorpay_payment_id VARCHAR(255) UNIQUE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10),
    status VARCHAR(50),
    method VARCHAR(50),
    captured BOOLEAN DEFAULT FALSE,
    error_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    payment_id INTEGER REFERENCES payments(id),
    business_id INTEGER REFERENCES businesses(id) NOT NULL,
    razorpay_refund_id VARCHAR(255) UNIQUE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webhook_events (
    id SERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_customers_business ON customers(business_id);
CREATE INDEX IF NOT EXISTS idx_customers_segment ON customers(segment);
CREATE INDEX IF NOT EXISTS idx_customers_intent ON customers(purchase_intent_score);
CREATE INDEX IF NOT EXISTS idx_customers_churn ON customers(churn_risk);
CREATE INDEX IF NOT EXISTS idx_products_business ON products(business_id);
CREATE INDEX IF NOT EXISTS idx_orders_business ON orders(business_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_carts_business ON carts(business_id);
CREATE INDEX IF NOT EXISTS idx_carts_customer ON carts(customer_id);
CREATE INDEX IF NOT EXISTS idx_carts_status ON carts(status);
CREATE INDEX IF NOT EXISTS idx_cust_events_cust ON customer_events(customer_id);
CREATE INDEX IF NOT EXISTS idx_cust_events_session ON customer_events(session_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_customer ON wishlist_items(customer_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_cust ON recommendations(customer_id);
CREATE INDEX IF NOT EXISTS idx_growth_opps_business ON growth_opportunities(business_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_business ON campaigns(business_id);
CREATE INDEX IF NOT EXISTS idx_agent_actions_business ON agent_actions(business_id);
