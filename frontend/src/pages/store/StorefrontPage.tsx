import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag, Search, Trash2, Plus, Minus, CreditCard } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { api, type StoreProduct } from '../../services/api';

declare global {
  interface Window { Razorpay?: new (options: Record<string, unknown>) => { open: () => void; on?: (event: string, handler: (payload: unknown) => void) => void }; }
}
const CART_KEY = 'growthpilot_store_cart';
type Cart = Record<number, number>;

export const StorefrontPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [cart, setCart] = useState<Cart>(() => JSON.parse(localStorage.getItem(CART_KEY) || '{}'));
  const [query, setQuery] = useState(new URLSearchParams(location.search).get('q') || '');
  const [customerId, setCustomerId] = useState('');
  const [message, setMessage] = useState('');
  const detailId = location.pathname.match(/^\/store\/products\/(\d+)$/)?.[1];
  const category = location.pathname.startsWith('/store/category/') ? decodeURIComponent(location.pathname.split('/').pop() || '') : undefined;
  const isCart = location.pathname === '/store/cart';
  const isSearch = location.pathname === '/store/search';
  const listQuery = useQuery({ queryKey: ['store', query, category], queryFn: () => api.getStoreProducts({ q: isSearch ? query : undefined, category }) });
  const detailQuery = useQuery({ queryKey: ['store-product', detailId], queryFn: () => api.getStoreProduct(Number(detailId)), enabled: !!detailId });
  const products = listQuery.data || [];
  const allCartProducts = useMemo(() => Object.entries(cart).filter(([, quantity]) => quantity > 0), [cart]);
  const total = allCartProducts.reduce((sum, [id, quantity]) => {
    const p = products.find(item => item.id === Number(id));
    return sum + (p ? p.price * quantity : 0);
  }, 0);

  useEffect(() => { localStorage.setItem(CART_KEY, JSON.stringify(cart)); }, [cart]);
  useEffect(() => { void api.trackStoreEvent({ eventType: 'SESSION_STARTED' }); }, []);
  useEffect(() => { if (isCart) void api.trackStoreEvent({ eventType: 'CART_VIEWED' }); }, [isCart]);
  useEffect(() => { if (detailId && detailQuery.data) void api.trackStoreEvent({ eventType: 'PRODUCT_VIEWED', productId: Number(detailId) }); }, [detailId, detailQuery.data]);
  useEffect(() => { if (isSearch && query) void api.trackStoreEvent({ eventType: 'SEARCH_PERFORMED', query }); }, [isSearch, query]);

  const change = (p: StoreProduct, delta: number) => {
    const quantity = Math.max(0, Math.min(p.stock || 0, (cart[p.id] || 0) + delta));
    setCart(c => ({ ...c, [p.id]: quantity }));
    if (delta > 0) void api.trackStoreEvent({ eventType: 'ADD_TO_CART', productId: p.id, customerId: customerId ? Number(customerId) : undefined });
  };
  const checkout = async () => {
    if (!allCartProducts.length) return setMessage('Your cart is empty.');
    void api.trackStoreEvent({ eventType: 'CHECKOUT_STARTED' });
    try {
      const order = await api.createCheckoutOrder({ customerId: customerId ? Number(customerId) : undefined, items: allCartProducts.map(([id, quantity]) => ({ productId: Number(id), quantity })) });
      void api.trackStoreEvent({ eventType: 'PAYMENT_STARTED', orderId: order.orderId });
      if (!window.Razorpay) return setMessage('Razorpay TEST Checkout is unavailable.');
      const payment = new window.Razorpay({ key: order.keyId, amount: Math.round(order.amount * 100), currency: order.currency, name: 'GrowthPilot Store', order_id: order.razorpayOrderId, description: 'TEST MODE order',
        handler: async (response: { razorpay_payment_id: string; razorpay_order_id: string; razorpay_signature: string }) => {
          try { await api.verifyCheckoutPayment({ orderId: order.orderId, razorpayOrderId: response.razorpay_order_id, razorpayPaymentId: response.razorpay_payment_id, razorpaySignature: response.razorpay_signature }); void api.trackStoreEvent({ eventType: 'PURCHASE_COMPLETED', orderId: order.orderId }); setCart({}); setMessage('Payment verified successfully.'); }
          catch (e) { void api.trackStoreEvent({ eventType: 'PAYMENT_FAILED', orderId: order.orderId }); setMessage(e instanceof Error ? e.message : 'Payment verification failed.'); }
        } }); payment.on?.('payment.failed', () => { void api.trackStoreEvent({ eventType: 'PAYMENT_FAILED', orderId: order.orderId }); setMessage('Payment failed. No charge was recorded.'); }); payment.open();
    } catch (e) { setMessage(e instanceof Error ? e.message : 'Checkout could not be started.'); }
  };
  const card = (p: StoreProduct) => <div key={p.id} className="glass-card rounded-2xl p-4 border border-slate-800"><Link to={`/store/products/${p.id}`} className="text-white font-bold hover:text-cyan-300">{p.name}</Link><p className="text-xs text-cyan-300 uppercase mt-2">{p.category}</p><p className="text-xs text-slate-400 mt-2 line-clamp-2">{p.description || 'Quality product'}</p><div className="flex justify-between items-center mt-4"><b className="text-white">₹{p.price.toLocaleString('en-IN')}</b><div className="flex items-center gap-2"><button onClick={() => change(p, -1)} className="p-1 bg-slate-800 rounded"><Minus size={14}/></button><span className="text-white">{cart[p.id] || 0}</span><button disabled={!p.stock} onClick={() => change(p, 1)} className="p-1 bg-indigo-600 rounded disabled:opacity-40"><Plus size={14}/></button></div></div><p className="text-[10px] text-slate-500 mt-2">{p.stock ? `${p.stock} in stock` : 'Out of stock'}</p></div>;
  if (detailId) { const p = detailQuery.data; return <div className="space-y-5"><Link to="/store/products" className="text-xs text-cyan-300">← Back to products</Link>{detailQuery.isLoading && <p className="text-slate-400">Loading product…</p>}{detailQuery.isError && <p className="text-rose-400">Product unavailable.</p>}{p && <div className="glass-card max-w-xl rounded-2xl p-6 border border-slate-800"><span className="text-xs text-cyan-300 uppercase">{p.category}</span><h1 className="text-2xl font-bold text-white mt-2">{p.name}</h1><p className="text-slate-300 mt-4">{p.description || 'Quality product'}</p><p className="text-xl font-black text-white mt-5">₹{p.price.toLocaleString('en-IN')}</p><button onClick={() => change(p, 1)} disabled={!p.stock} className="mt-5 rounded-xl bg-cyan-600 px-4 py-3 text-white font-bold disabled:opacity-40">Add to cart</button></div>}</div>; }
  return <div className="space-y-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-extrabold text-white flex items-center gap-2"><ShoppingBag className="text-cyan-400"/> Storefront</h1><p className="text-xs text-slate-400">Razorpay TEST MODE only.</p></div><Link to="/store/cart" className="text-sm text-cyan-300">Cart ({Object.values(cart).reduce((a, b) => a + b, 0)})</Link></div><div className="flex gap-2"><input value={query} onChange={e => setQuery(e.target.value)} onKeyDown={e => e.key === 'Enter' && navigate(`/store/search?q=${encodeURIComponent(query)}`)} placeholder="Search products" className="flex-1 rounded-xl bg-slate-950 border border-slate-700 p-3 text-sm text-white"/><button onClick={() => navigate(`/store/search?q=${encodeURIComponent(query)}`)} className="rounded-xl bg-indigo-600 px-4 text-white"><Search size={18}/></button></div>{isCart ? <div className="max-w-xl space-y-3">{allCartProducts.map(([id, quantity]) => { const p = products.find(x => x.id === Number(id)); return p ? <div key={id} className="flex justify-between items-center rounded-xl border border-slate-800 p-3 text-white"><span>{p.name} × {quantity}</span><button onClick={() => setCart(c => ({ ...c, [p.id]: 0 }))}><Trash2 size={16} className="text-rose-400"/></button></div> : null; })}<div className="text-white font-bold">Total ₹{total.toLocaleString('en-IN')}</div><input value={customerId} onChange={e => setCustomerId(e.target.value.replace(/\D/g, ''))} placeholder="Demo customer ID (optional)" className="w-full rounded-xl bg-slate-950 border border-slate-700 p-3 text-xs text-white"/><button onClick={checkout} className="w-full rounded-xl bg-cyan-600 p-3 text-white font-bold flex justify-center gap-2"><CreditCard size={17}/> Pay securely</button>{message && <p className="text-xs text-amber-300">{message}</p>}</div> : <>{listQuery.isLoading && <p className="text-slate-400">Loading catalog…</p>}{listQuery.isError && <p className="text-rose-400">Unable to load catalog.</p>}{!listQuery.isLoading && !products.length && <p className="text-slate-400">No products found.</p>}<div className="grid gap-4 md:grid-cols-3">{products.map(card)}</div></>}</div>;
};
