export const API_BASE_URL =
  (import.meta as any).env?.VITE_API_BASE_URL ||
  (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'
    ? 'https://ai-growth-agentic-commerse.onrender.com/api'
    : 'http://localhost:8080/api');

export const fetchApi = async <T>(endpoint: string, options: RequestInit = {}): Promise<T> => {
  const token = localStorage.getItem('growthpilot_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string> || {}),
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401 && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
      localStorage.removeItem('growthpilot_token');
      localStorage.removeItem('growthpilot_user');
      window.location.href = '/login';
    }
    const errorData = await response.json().catch(() => null);
    throw new Error(errorData?.message || `API Error: ${response.statusText}`);
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : null) as T;
};

// Types & API functions
export interface AuthResponse {
  token: string;
  email: string;
  role: string;
  businessId: number;
  businessName: string;
}

export interface UserProfile {
  id: number;
  email: string;
  role: string;
  businessId: number;
  businessName: string;
  businessDescription: string;
  createdAt: string;
}

export interface DashboardSummary {
  totalRevenue: number;
  totalOrders: number;
  totalCustomers: number;
  totalProducts: number;
  averageOrderValue: number;
  activeCustomers: number;
  repeatCustomers: number;
  abandonedCarts: number;
  recoverableCartRevenue: number;
  revenueAtRisk: number;
  highIntentCustomers: number;
  growthOpportunities: number;
  pendingApprovals: number;
  overallConversionRate: number;
  cartRecoveryRate: number;
  revenueTrend: { date: string; revenue: number; orders: number }[];
  ordersTrend: { date: string; revenue: number; orders: number }[];
  customerSegments: { segment: string; count: number; percentage: number }[];
  topProducts: {
    id: number;
    name: string;
    category: string;
    price: number;
    totalSales: number;
    totalRevenue: number;
    views: number;
    cartAdditions: number;
    conversionRate: number;
  }[];
  conversionFunnel: { stage: string; count: number; conversionRate: number }[];
}

export interface IntentFactor {
  signalName: string;
  scoreContribution: number;
  maxScore: number;
  status: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE';
  explanation: string;
}

export interface RecommendationItem {
  id: number;
  productId: number;
  productName: string;
  productCategory: string;
  productPrice: number;
  score: number;
  reason: string;
}

export interface CustomerSummary {
  id: number;
  name: string;
  email: string;
  phone: string;
  segment: string;
  preferredCategory: string;
  totalOrders: number;
  totalSpend: number;
  averageOrderValue: number;
  lastOrderDate: string;
  engagementScore: number;
  purchaseIntentScore: number;
  churnRisk: string;
  createdAt: string;
}

export interface CustomerIntelligence {
  id: number;
  name: string;
  email: string;
  phone: string;
  segment: string;
  preferredCategory: string;
  totalOrders: number;
  totalSpend: number;
  averageOrderValue: number;
  lastOrderDate: string;
  daysSinceLastOrder: number;
  purchaseIntentScore: number;
  purchaseIntentStatus: string;
  intentFactors: IntentFactor[];
  churnRisk: string;
  churnReason: string;
  suggestedRetentionAction: string;
  engagementScore: number;
  recommendations: RecommendationItem[];
  recentOrders: {
    id: number;
    totalAmount: number;
    status: string;
    orderDate: string;
    itemCount: number;
  }[];
}

export interface ProductIntelligence {
  id: number;
  name: string;
  description: string;
  category: string;
  price: number;
  stock: number;
  totalSales: number;
  totalRevenue: number;
  views: number;
  cartAdditions: number;
  conversionRate: number;
  cartConversionRate: number;
  performanceBadge: 'TRENDING' | 'HIGH_CONVERTING' | 'LOW_PERFORMING' | 'STABLE';
  aiInsight: string;
  crossSellCandidates: {
    id: number;
    name: string;
    category: string;
    price: number;
    affinityScore: number;
    reason: string;
  }[];
  bundleOpportunities: {
    id: number;
    name: string;
    category: string;
    price: number;
    affinityScore: number;
    reason: string;
  }[];
  createdAt: string;
}

export interface CartRecovery {
  cartId: number;
  customerId: number;
  customerName: string;
  customerEmail: string;
  customerSegment: string;
  churnRisk: string;
  purchaseIntentScore: number;
  items: {
    productId: number;
    productName: string;
    category: string;
    price: number;
    quantity: number;
    subtotal: number;
  }[];
  totalValue: number;
  recoveryProbability: number;
  recommendedIncentive: string;
  suggestedMessage: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface GrowthOpportunity {
  id: number;
  title: string;
  type: 'RECOVER_CART' | 'HIGH_INTENT' | 'PREVENT_CHURN' | 'UPSELL' | 'BUNDLE';
  targetCustomerId?: number;
  targetCustomerName?: string;
  targetProductId?: number;
  targetProductName?: string;
  estimatedImpact: number;
  confidenceScore: number;
  recommendedAction: string;
  reason: string;
  status: string;
  createdAt: string;
}

export interface Campaign {
  id: number;
  name: string;
  targetSegment: string;
  status: 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'ACTIVE' | 'COMPLETED' | 'REJECTED';
  offerDetails: string;
  generatedMessage: string;
  estimatedImpact: number;
  createdByAgent: boolean;
  createdAt: string;
}

export interface AgentAction {
  id: number;
  businessId: number;
  userEmail: string;
  goal: string;
  reasoningSummary: string;
  toolUsed: string;
  parameters: string;
  result: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED';
  createdAt: string;
}

export interface AiChatResponse {
  response: string;
  proposedActions: {
    title: string;
    actionType: string;
    description: string;
    estimatedImpact: string;
    targetEntity: string;
    suggestedParameters: string;
    requiresApproval: boolean;
  }[];
  toolsInvoked: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CheckoutOrder {
  orderId: number;
  razorpayOrderId: string;
  amount: number;
  currency: string;
  keyId: string;
  status: string;
}
export type StoreProduct = Pick<ProductIntelligence, 'id' | 'name' | 'description' | 'category' | 'price' | 'stock'>;

export interface PasswordResetResponse {
  success: boolean;
  message: string;
  email: string;
  demoOtp?: string;
}

// API Service Functions
export const api = {
  register: (data: { email: string; password: string; businessName?: string }) =>
    fetchApi<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify(data) }),

  login: (data: { email: string; password: string }) =>
    fetchApi<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify(data) }),

  forgotPassword: (email: string) =>
    fetchApi<PasswordResetResponse>('/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) }),

  verifyResetCode: (email: string, code: string) =>
    fetchApi<PasswordResetResponse>('/auth/verify-code', { method: 'POST', body: JSON.stringify({ email, code }) }),

  resetPassword: (data: { email: string; code: string; newPassword: string }) =>
    fetchApi<PasswordResetResponse>('/auth/reset-password', { method: 'POST', body: JSON.stringify(data) }),

  getMe: () => fetchApi<UserProfile>('/auth/me'),

  getWorkspaceProfile: () => fetchApi<UserProfile>('/workspace/profile'),
  updateWorkspaceProfile: (data: { name?: string; description?: string }) =>
    fetchApi<UserProfile>('/workspace/profile', { method: 'PUT', body: JSON.stringify(data) }),

  getDashboardSummary: () => fetchApi<DashboardSummary>('/dashboard/summary'),

  getCustomers: (params?: { query?: string; segment?: string; churnRisk?: string; page?: number; size?: number }) => {
    const queryStr = new URLSearchParams();
    if (params?.query) queryStr.append('query', params.query);
    if (params?.segment) queryStr.append('segment', params.segment);
    if (params?.churnRisk) queryStr.append('churnRisk', params.churnRisk);
    if (params?.page !== undefined) queryStr.append('page', params.page.toString());
    if (params?.size !== undefined) queryStr.append('size', params.size.toString());
    return fetchApi<PageResponse<CustomerSummary>>(`/customers?${queryStr.toString()}`);
  },

  getCustomerIntelligence: (id: number) => fetchApi<CustomerIntelligence>(`/customers/${id}/intelligence`),
  getHighIntentCustomers: () => fetchApi<CustomerSummary[]>('/customers/high-intent'),

  getProducts: (params?: { category?: string; page?: number; size?: number }) => {
    const queryStr = new URLSearchParams();
    if (params?.category) queryStr.append('category', params.category);
    if (params?.page !== undefined) queryStr.append('page', params.page.toString());
    if (params?.size !== undefined) queryStr.append('size', params.size.toString());
    return fetchApi<PageResponse<ProductIntelligence>>(`/products?${queryStr.toString()}`);
  },

  getProductIntelligence: (id: number) => fetchApi<ProductIntelligence>(`/products/${id}/intelligence`),

  getRecommendations: (customerId: number) => fetchApi<RecommendationItem[]>(`/recommendations/customer/${customerId}`),
  regenerateRecommendations: (customerId: number) => fetchApi<RecommendationItem[]>(`/recommendations/generate/${customerId}`, { method: 'POST' }),

  getAbandonedCarts: (page = 0, size = 10) => fetchApi<PageResponse<CartRecovery>>(`/carts/abandoned?page=${page}&size=${size}`),
  getAbandonedCartsList: () => fetchApi<CartRecovery[]>('/carts/abandoned/list'),
  getCartRecoveryPlan: (id: number) => fetchApi<CartRecovery>(`/carts/${id}/recovery-plan`),
  triggerCartRecovery: (id: number, data: { discountPercent?: number; message?: string; executeDirectly?: boolean }) =>
    fetchApi<AgentAction>(`/carts/${id}/recover`, { method: 'POST', body: JSON.stringify(data) }),

  getOpportunities: () => fetchApi<GrowthOpportunity[]>('/opportunities'),
  generateOpportunities: () => fetchApi<GrowthOpportunity[]>('/opportunities/generate', { method: 'POST' }),
  executeOpportunity: (id: number, data?: { customNotes?: string; autoApprove?: boolean }) =>
    fetchApi<AgentAction>(`/opportunities/${id}/execute`, { method: 'POST', body: JSON.stringify(data || {}) }),

  getCampaigns: (page = 0, size = 10) => fetchApi<PageResponse<Campaign>>(`/campaigns?page=${page}&size=${size}`),
  createCampaign: (data: { name: string; targetSegment: string; offerDetails?: string; generatedMessage?: string; estimatedImpact?: number; status?: string }) =>
    fetchApi<Campaign>('/campaigns', { method: 'POST', body: JSON.stringify(data) }),
  updateCampaignStatus: (id: number, status: string) =>
    fetchApi<Campaign>(`/campaigns/${id}/status?status=${status}`, { method: 'PATCH' }),

  copilotChat: (message: string) =>
    fetchApi<AiChatResponse>('/agent/chat', { method: 'POST', body: JSON.stringify({ message }) }),

  getAgentActions: (status?: string, page = 0, size = 20) => {
    const queryStr = new URLSearchParams();
    if (status) queryStr.append('status', status);
    queryStr.append('page', page.toString());
    queryStr.append('size', size.toString());
    return fetchApi<PageResponse<AgentAction>>(`/agent/actions?${queryStr.toString()}`);
  },

  approveAgentAction: (id: number) => fetchApi<AgentAction>(`/agent/actions/${id}/approve`, { method: 'POST' }),
  rejectAgentAction: (id: number) => fetchApi<AgentAction>(`/agent/actions/${id}/reject`, { method: 'POST' }),

  // Razorpay Integration
  getRazorpayStatus: () => fetchApi<any>('/integrations/razorpay/status'),
  testRazorpayConnection: (data: { keyId: string; keySecret: string }) => 
    fetchApi<any>('/integrations/razorpay/test', { method: 'POST', body: JSON.stringify(data) }),
  saveRazorpayCredentials: (data: { keyId: string; keySecret: string; webhookSecret?: string }) => 
    fetchApi<any>('/integrations/razorpay/save-credentials', { method: 'POST', body: JSON.stringify(data) }),
  syncRazorpay: () => fetchApi<any>('/integrations/razorpay/sync', { method: 'POST', body: JSON.stringify({}) }),

  createCheckoutOrder: (data: { customerId?: number; items: { productId: number; quantity: number }[] }) =>
    fetchApi<CheckoutOrder>('/checkout/orders', { method: 'POST', body: JSON.stringify(data) }),
  verifyCheckoutPayment: (data: { orderId: number; razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
    fetchApi<CheckoutOrder>('/checkout/verify', { method: 'POST', body: JSON.stringify(data) }),
  getStoreProducts: (params?: { q?: string; category?: string }) => {
    const query = new URLSearchParams();
    if (params?.q) query.set('q', params.q);
    if (params?.category) query.set('category', params.category);
    return fetchApi<StoreProduct[]>(`/store/products?${query.toString()}`);
  },
  getStoreProduct: (id: number) => fetchApi<StoreProduct>(`/store/products/${id}`),
  trackStoreEvent: (data: { eventType: string; customerId?: number; productId?: number; query?: string; orderId?: number }) =>
    fetchApi<void>('/store/events', { method: 'POST', body: JSON.stringify(data) }).catch(() => undefined),

  // Development & Testing Tools
  seedDemoData: () => fetchApi<{ success: boolean; message: string }>('/workspace/seed-demo', { method: 'POST' }),
};
