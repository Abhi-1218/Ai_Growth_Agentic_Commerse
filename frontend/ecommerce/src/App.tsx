import { FormEvent, ReactNode, useEffect, useMemo, useState } from 'react';
import { Link, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowRight, Heart, Menu, Search, ShoppingBag, Sparkles, UserRound, X } from 'lucide-react';

type Product = {
  id: number; name: string; description?: string; category: string; brand?: string;
  sku?: string; imageUrl?: string; metadata?: string; price: number; stock: number;
};
type CartItem = { productId: number; name: string; price: number; quantity: number; subtotal: number };
type Cart = { cartId: number | null; status: string; items: CartItem[]; total: number };
type Order = {
  id: number; status: string; totalAmount: number; currency: string; orderDate: string;
  items: { productId: number; name: string; quantity: number; price: number }[];
};
type Profile = { customerId: string; id: number; name: string; email: string; phone?: string };

const API = '/api';
const sessionId = localStorage.getItem('techmart_session') || crypto.randomUUID();
localStorage.setItem('techmart_session', sessionId);

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('techmart_token');
  const headers = new Headers(options.headers);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(API + path, { ...options, headers });
  if (!response.ok) {
    const payload = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(payload?.message || `Request failed (${response.status})`);
  }
  return response.status === 204 ? null as T : response.json();
}

function track(eventType: string, extra: Record<string, unknown> = {}) {
  if (localStorage.getItem('techmart_token')) {
    void request('/store/events', {
      method: 'POST',
      body: JSON.stringify({ eventType, sessionId, ...extra }),
    }).catch(() => undefined);
  }
}
function startTrackingSession() {
  if (!localStorage.getItem('techmart_session_started')) {
    localStorage.setItem('techmart_session_started', sessionId);
    track('SESSION_STARTED');
  }
}
const money = (value: number) => `₹${Number(value || 0).toLocaleString('en-IN')}`;
const image = (p: Product) => p.imageUrl || 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=85';

function Shell({ children, cartCount }: { children: ReactNode; cartCount: number }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState('');
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (query.trim()) {
      track('SEARCH_PERFORMED', { query: query.trim() });
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
      setMenuOpen(false);
    }
  };
  return <>
    <div className="announcement"><Sparkles size={14} /> Free delivery on orders over ₹999 <span>•</span> 7-day easy returns</div>
    <header>
      <Link className="brand" to="/"><span className="brand-mark">T</span> tech<span>mart</span></Link>
      <nav className={menuOpen ? 'nav-open' : ''}>
        <Link className={location.pathname === '/' ? 'active' : ''} to="/" onClick={() => setMenuOpen(false)}>Home</Link>
        <Link to="/products" onClick={() => setMenuOpen(false)}>All products</Link>
        <Link to="/category/Electronics" onClick={() => setMenuOpen(false)}>Electronics</Link>
        <Link to="/category/Footwear" onClick={() => setMenuOpen(false)}>Footwear</Link>
        <Link to="/category/Accessories" onClick={() => setMenuOpen(false)}>Accessories</Link>
      </nav>
      <form className="search" onSubmit={submit}><Search size={18} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search products" aria-label="Search products" /></form>
      <div className="header-actions">
        <Link to="/wishlist" aria-label="Wishlist"><Heart size={20} /></Link>
        <Link to="/profile" aria-label="Account"><UserRound size={20} /></Link>
        <Link to="/cart" className="cart-link" aria-label="Cart"><ShoppingBag size={20} /><b>{cartCount}</b></Link>
        <button className="menu" onClick={() => setMenuOpen(!menuOpen)} aria-label="Toggle menu">{menuOpen ? <X /> : <Menu />}</button>
      </div>
    </header>
    <main>{children}</main>
    <footer><div className="brand"><span className="brand-mark">T</span> tech<span>mart</span></div><p>Smart tech for everyday life.</p><small>© 2026 TechMart · Razorpay Test Mode checkout</small></footer>
  </>;
}

function ProductCard({ product, add, wished, toggle }: { product: Product; add: (p: Product) => void; wished: boolean; toggle: (id: number) => void }) {
  return <article className="product-card">
    <Link to={`/product/${product.id}`}>
      <div className="product-art"><img src={image(product)} alt={product.name} loading="lazy" /><span>{product.category}</span></div>
      <div className="product-info"><div><h3>{product.name}</h3><p>{product.description || 'Built for your everyday.'}</p></div>
        <button type="button" className="heart" aria-label={wished ? 'Remove from wishlist' : 'Save product'} onClick={e => { e.preventDefault(); toggle(product.id); }}><Heart size={18} fill={wished ? 'currentColor' : 'none'} /></button>
        <b className="price">{money(product.price)}</b>
      </div>
    </Link>
    <button className="add" disabled={!product.stock} onClick={() => add(product)}>{product.stock ? 'Add to bag' : 'Out of stock'} <ArrowRight size={16} /></button>
  </article>;
}

function Catalog({ products, add, wished, toggle, title }: { products: Product[]; add: (p: Product) => void; wished: Set<number>; toggle: (id: number) => void; title: string }) {
  return <section className="catalog"><div className="section-heading"><div><p className="eyebrow">Curated for you</p><h2>{title}</h2></div><span>{products.length} products</span></div>
    {products.length ? <div className="product-grid">{products.map(product => <ProductCard key={product.id} product={product} add={add} wished={wished.has(product.id)} toggle={toggle} />)}</div> : <div className="empty"><h2>No products found</h2><p>Try another search or browse the full collection.</p><Link className="primary" to="/products">Browse products</Link></div>}
  </section>;
}

function Home({ products, add, wished, toggle }: { products: Product[]; add: (p: Product) => void; wished: Set<number>; toggle: (id: number) => void }) {
  return <><section className="hero"><div><p className="eyebrow">Technology, thoughtfully chosen</p><h1>Good tech.<br /><em>Better living.</em></h1><p className="hero-copy">Discover reliable devices and accessories that fit your life, not the other way around.</p><Link className="primary" to="/category/Electronics">Shop the collection <ArrowRight size={18} /></Link></div><div className="hero-art"><span>NEW ARRIVAL</span><strong>Make room<br />for better.</strong></div></section>
    <div className="perks"><div>⚡ <b>Fast dispatch</b><small>Ships within 24 hours</small></div><div>✓ <b>Tested quality</b><small>Every item is verified</small></div><div>↺ <b>Easy returns</b><small>7 days, no questions</small></div></div>
    <Catalog products={products.slice(0, 8)} add={add} wished={wished} toggle={toggle} title="Featured tech" /></>;
}

function Account() {
  const navigate = useNavigate();
  const signup = useLocation().pathname === '/signup';
  const [name, setName] = useState(''); const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [error, setError] = useState('');
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError('');
    try {
      const data = await request<{ token: string; email: string; customerId: string }>(`/customer-auth/${signup ? 'register' : 'login'}`, { method: 'POST', body: JSON.stringify(signup ? { name, email, password } : { email, password }) });
      localStorage.setItem('techmart_token', data.token); localStorage.setItem('techmart_customer_email', data.email);
      startTrackingSession();
      track(signup ? 'ACCOUNT_CREATED' : 'LOGIN_COMPLETED', { customerId: data.customerId });
      navigate('/');
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to continue'); }
  };
  return <section className="auth"><div className="auth-intro"><p className="eyebrow">Welcome to TechMart</p><h1>Everything you need.<br /><em>Nothing you don't.</em></h1><p>Create an account to save your bag and enjoy a faster checkout.</p></div>
    <form className="auth-card" onSubmit={submit}><h2>{signup ? 'Create your account' : 'Welcome back'}</h2>{signup && <input placeholder="Your name" value={name} onChange={e => setName(e.target.value)} required />}<input type="email" placeholder="Email address" value={email} onChange={e => setEmail(e.target.value)} required /><input type="password" minLength={8} placeholder="Password (8+ characters)" value={password} onChange={e => setPassword(e.target.value)} required /><button className="primary">{signup ? 'Create account' : 'Sign in'} <ArrowRight size={17} /></button>{error && <p className="error">{error}</p>}<button type="button" className="switch" onClick={() => navigate(signup ? '/login' : '/signup')}>{signup ? 'Already have an account? Sign in' : 'New to TechMart? Create an account'}</button></form>
  </section>;
}

function CartPage({ cart, setCart }: { cart: Cart | null; setCart: (c: Cart) => void }) {
  const navigate = useNavigate(); const [message, setMessage] = useState('');
  if (!localStorage.getItem('techmart_token')) return <section className="empty"><h1>Sign in to view your bag</h1><Link className="primary" to="/login">Sign in</Link></section>;
  const update = async (id: number, quantity: number) => {
    try { const next = await request<Cart>(`/customer/cart/${id}?sessionId=${encodeURIComponent(sessionId)}${quantity > 0 ? `&quantity=${quantity}` : ''}`, { method: quantity > 0 ? 'PUT' : 'DELETE' }); setCart(next); }
    catch (e) { setMessage(e instanceof Error ? e.message : 'Unable to update cart.'); }
  };
  const checkout = async () => {
    if (!cart?.items.length) return; setMessage('');
    try {
      const order = await request<{ orderId: number; razorpayOrderId: string; amount: number; currency: string; keyId: string }>('/checkout/orders', { method: 'POST', body: JSON.stringify({ items: cart.items.map(i => ({ productId: i.productId, quantity: i.quantity })), sessionId }) });
      track('PAYMENT_STARTED', { orderId: order.orderId });
      const Razorpay = (window as unknown as { Razorpay?: new (options: Record<string, unknown>) => { open: () => void } }).Razorpay;
      if (!Razorpay) throw new Error('Razorpay checkout script is unavailable.');
      new Razorpay({ key: order.keyId, amount: Math.round(order.amount * 100), currency: order.currency, order_id: order.razorpayOrderId, name: 'TechMart', description: 'TechMart Test Mode order',
        handler: async (result: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string }) => {
          try { await request('/checkout/verify', { method: 'POST', body: JSON.stringify({ orderId: order.orderId, razorpayOrderId: result.razorpay_order_id, razorpayPaymentId: result.razorpay_payment_id, razorpaySignature: result.razorpay_signature, sessionId }) }); track('PURCHASE_COMPLETED', { orderId: order.orderId }); navigate('/order-success'); }
          catch (e) { track('PAYMENT_FAILED', { orderId: order.orderId }); await request(`/checkout/${order.orderId}/failure?reason=${encodeURIComponent(e instanceof Error ? e.message : 'Verification failed')}&sessionId=${encodeURIComponent(sessionId)}`, { method: 'POST' }).catch(() => undefined); setMessage(e instanceof Error ? e.message : 'Payment verification failed.'); }
        },
        modal: { ondismiss: () => { track('PAYMENT_FAILED', { orderId: order.orderId }); void request(`/checkout/${order.orderId}/failure?reason=${encodeURIComponent('Checkout dismissed by customer')}&sessionId=${encodeURIComponent(sessionId)}`, { method: 'POST' }).catch(() => undefined); setMessage('Payment cancelled; your cart was saved as an abandoned cart.'); } },
      }).open();
    } catch (e) { setMessage(e instanceof Error ? e.message : 'Unable to start checkout.'); }
  };
  return <section className="cart-page"><div><p className="eyebrow">Your account</p><h1>Your bag</h1>{cart?.items.length ? cart.items.map(item => <div className="cart-row" key={item.productId}><div><b>{item.name}</b><small>{money(item.price)} each</small></div><div className="quantity"><button onClick={() => update(item.productId, item.quantity - 1)} aria-label="Decrease quantity">−</button><span>{item.quantity}</span><button onClick={() => update(item.productId, item.quantity + 1)} aria-label="Increase quantity">+</button></div><b>{money(item.subtotal)}</b><button className="switch" onClick={() => update(item.productId, 0)}>Remove</button></div>) : <p>Your bag is waiting for something great.</p>}</div>
    <aside className="cart-summary"><h2>Order summary</h2><div><span>Subtotal</span><b>{money(cart?.total || 0)}</b></div><small>Secure payment via Razorpay Test Mode. Your cart is saved if checkout is cancelled.</small>{cart?.items.length ? <button className="primary" onClick={checkout}>Checkout securely <ArrowRight size={17} /></button> : <Link className="primary" to="/products">Continue shopping</Link>}{message && <p className="error">{message}</p>}</aside>
  </section>;
}

function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  useEffect(() => { request<Order[]>('/customer/orders').then(setOrders).catch(() => setOrders([])); }, []);
  return <section className="empty"><p className="eyebrow">Your account</p><h1>Orders</h1>{orders.length ? orders.map(order => <Link className="order-row" key={order.id} to={`/orders/${order.id}`}><b>Order #{order.id}</b><span>{order.status} · {money(order.totalAmount)}</span></Link>) : <p>No orders yet.</p>}<Link className="primary" to="/products">Shop products</Link></section>;
}

function OrderDetail() {
  const { id } = useParams(); const [order, setOrder] = useState<Order | null>(null);
  useEffect(() => { if (id) request<Order>(`/customer/orders/${id}`).then(setOrder).catch(() => setOrder(null)); }, [id]);
  return <section className="empty">{order ? <><p className="eyebrow">Order #{order.id}</p><h1>{order.status}</h1>{order.items.map(item => <p key={item.productId}>{item.name} × {item.quantity} · {money(item.price * item.quantity)}</p>)}<h2>{money(order.totalAmount)}</h2></> : <h1>Order not found</h1>}<Link className="primary" to="/orders">Back to orders</Link></section>;
}

function Profile() {
  const navigate = useNavigate(); const [profile, setProfile] = useState<Profile | null>(null); const [name, setName] = useState(''); const [phone, setPhone] = useState(''); const [message, setMessage] = useState('');
  useEffect(() => { if (localStorage.getItem('techmart_token')) request<Profile>('/customer-auth/me').then(p => { setProfile(p); setName(p.name); setPhone(p.phone || ''); }).catch(() => setProfile(null)); }, []);
  if (!profile) return <section className="empty"><h1>Sign in to view your profile</h1><Link className="primary" to="/login">Sign in</Link></section>;
  return <section className="profile"><div><p className="eyebrow">Account</p><h1>Your profile</h1><p className="muted">Customer ID: {profile.customerId}</p><Link to="/orders">View order history</Link></div><form className="auth-card" onSubmit={async e => { e.preventDefault(); try { setProfile(await request<Profile>('/customer-auth/me', { method: 'PUT', body: JSON.stringify({ name, phone }) })); setMessage('Profile saved.'); } catch (error) { setMessage(error instanceof Error ? error.message : 'Unable to save profile.'); } }}><input value={name} onChange={e => setName(e.target.value)} required /><input value={profile.email} readOnly /><input placeholder="Phone" value={phone} onChange={e => setPhone(e.target.value)} /><button className="primary">Save profile</button>{message && <p className="muted">{message}</p>}<button type="button" className="switch" onClick={() => { localStorage.removeItem('techmart_token'); localStorage.removeItem('techmart_customer_email'); navigate('/login'); }}>Sign out</button></form></section>;
}

function ProductPage({ products, add, wished, toggle }: { products: Product[]; add: (p: Product) => void; wished: Set<number>; toggle: (id: number) => void }) {
  const { id } = useParams(); const [product, setProduct] = useState<Product | null>(products.find(p => p.id === Number(id)) || null);
  useEffect(() => { if (id) { track('PRODUCT_VIEWED', { productId: Number(id) }); request<Product>(`/store/products/${id}`).then(setProduct).catch(() => setProduct(null)); } }, [id]);
  if (!product) return <div className="empty">Product not found.</div>;
  return <section className="detail"><div className="detail-art"><img src={image(product)} alt={product.name} /><span>{product.category} · {product.brand || 'TechMart'}</span></div><div className="detail-copy"><p className="eyebrow">{product.category}</p><h1>{product.name}</h1><p className="description">{product.description || 'Thoughtfully designed for performance, comfort and reliability.'}</p><b className="detail-price">{money(product.price)}</b><p className="stock">{product.stock ? `In stock · ${product.stock} ready to ship` : 'Currently unavailable'}</p><button className="primary" disabled={!product.stock} onClick={() => add(product)}>Add to bag <ShoppingBag size={18} /></button><button className="switch" onClick={() => toggle(product.id)}><Heart size={16} fill={wished.has(product.id) ? 'currentColor' : 'none'} /> {wished.has(product.id) ? 'Saved' : 'Save for later'}</button><div className="metadata"><b>SKU {product.sku || '—'}</b><span>Manufacturer warranty · 7-day returns</span></div></div></section>;
}

function App() {
  const [products, setProducts] = useState<Product[]>([]); const [cart, setCart] = useState<Cart | null>(null); const [wishlist, setWishlist] = useState<Product[]>([]); const location = useLocation(); const navigate = useNavigate();
  useEffect(() => { if (localStorage.getItem('techmart_token')) startTrackingSession(); }, []);
  useEffect(() => { const category = location.pathname.startsWith('/category/') ? `?category=${encodeURIComponent(location.pathname.split('/').pop() || '')}` : location.pathname === '/search' ? `?q=${encodeURIComponent(new URLSearchParams(location.search).get('q') || '')}` : ''; request<Product[]>(`/store/products${category}`).then(setProducts).catch(() => setProducts([])); }, [location.pathname, location.search]);
  useEffect(() => { if (localStorage.getItem('techmart_token')) { request<Cart>(`/customer/cart?sessionId=${encodeURIComponent(sessionId)}`).then(setCart).catch(() => undefined); request<Product[]>('/customer/wishlist').then(setWishlist).catch(() => undefined); } }, []);
  const add = async (product: Product) => { if (!localStorage.getItem('techmart_token')) { navigate('/login'); return; } try { const current = cart?.items.find(item => item.productId === product.id)?.quantity || 0; setCart(await request<Cart>(`/customer/cart/${product.id}?quantity=${current + 1}&sessionId=${encodeURIComponent(sessionId)}`, { method: 'PUT' })); } catch (e) { if ((e as Error).message.toLowerCase().includes('unauth')) navigate('/login'); } };
  const toggle = async (id: number) => { if (!localStorage.getItem('techmart_token')) { navigate('/login'); return; } try { setWishlist(await request<Product[]>(`/customer/wishlist/${id}`, { method: wishlist.some(p => p.id === id) ? 'DELETE' : 'POST' })); } catch { navigate('/login'); } };
  const wished = useMemo(() => new Set(wishlist.map(p => p.id)), [wishlist]); const count = cart?.items.reduce((sum, item) => sum + item.quantity, 0) || 0;
  return <Shell cartCount={count}><Routes><Route path="/" element={<Home products={products} add={add} wished={wished} toggle={toggle} />} /><Route path="/products" element={<Catalog products={products} add={add} wished={wished} toggle={toggle} title="All products" />} /><Route path="/category/:category" element={<Catalog products={products} add={add} wished={wished} toggle={toggle} title={`${location.pathname.split('/').pop()} collection`} />} /><Route path="/search" element={<Catalog products={products} add={add} wished={wished} toggle={toggle} title="Search results" />} /><Route path="/product/:id" element={<ProductPage products={products} add={add} wished={wished} toggle={toggle} />} /><Route path="/login" element={<Account />} /><Route path="/signup" element={<Account />} /><Route path="/profile" element={<Profile />} /><Route path="/wishlist" element={<Catalog products={wishlist} add={add} wished={wished} toggle={toggle} title="Wishlist" />} /><Route path="/orders" element={<Orders />} /><Route path="/orders/:id" element={<OrderDetail />} /><Route path="/cart" element={<CartPage cart={cart} setCart={setCart} />} /><Route path="/checkout" element={<CartPage cart={cart} setCart={setCart} />} /><Route path="/order-success" element={<section className="empty"><p className="eyebrow">Order confirmed</p><h1>Thank you for your order.</h1><Link className="primary" to="/orders">View orders <ArrowRight size={17} /></Link></section>} /></Routes></Shell>;
}
export default App;
