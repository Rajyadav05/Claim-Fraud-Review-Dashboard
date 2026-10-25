/**
 * ClaimGuard REST API Client & Session Manager
 * Connects the frontend UI to Spring Boot endpoints with CSRF, Session, and Role Support.
 */
const API = {
    baseUrl: '/api',

    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    },

    _csrfData: null,
    _csrfPromise: null,

    async fetchCsrf() {
        try {
            const res = await fetch(`${this.baseUrl}/auth/csrf`);
            if (res.ok) {
                this._csrfData = await res.json();
                return this._csrfData;
            }
        } catch (e) {}
        return null;
    },

    async getCsrfToken() {
        if (this._csrfData && this._csrfData.token) {
            return this._csrfData.token;
        }
        if (!this._csrfPromise) {
            this._csrfPromise = this.fetchCsrf();
        }
        const data = await this._csrfPromise;
        if (data && data.token) {
            return data.token;
        }
        return this.getCookie('XSRF-TOKEN');
    },

    async _request(url, options = {}) {
        const headers = options.headers || {};
        const method = (options.method || 'GET').toUpperCase();

        // Automatically attach CSRF token for mutating state requests
        if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
            if (!headers['X-XSRF-TOKEN']) {
                const xsrfToken = await this.getCsrfToken();
                if (xsrfToken) {
                    headers['X-XSRF-TOKEN'] = xsrfToken;
                }
            }
        }

        options.headers = headers;

        const res = await fetch(url, options);

        // Handle unauthenticated session
        if (res.status === 401) {
            const currentPath = window.location.pathname + window.location.search;
            if (!window.location.pathname.includes('login.html')) {
                window.location.href = `/login.html?redirect=${encodeURIComponent(currentPath)}`;
            }
            throw new Error('Authentication session expired. Redirecting to login.');
        }

        // Handle forbidden action
        if (res.status === 403) {
            const errData = await res.json().catch(() => ({}));
            const err = new Error(errData.detail || 'Access denied: You do not have permissions for this action.');
            err.status = 403;
            throw err;
        }

        return res;
    },

    async getHealth() {
        const res = await this._request(`${this.baseUrl}/health`);
        return res.json();
    },

    async getCurrentUser() {
        try {
            const res = await this._request(`${this.baseUrl}/auth/me`);
            return res.json();
        } catch (err) {
            return { authenticated: false };
        }
    },

    async logout() {
        try {
            await this._request(`${this.baseUrl}/auth/logout`, { method: 'POST' });
        } finally {
            window.location.href = '/login.html';
        }
    },

    async getClaims({ status, risk, search, page = 0, size = 15 } = {}) {
        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (risk) params.append('risk', risk);
        if (search) params.append('search', search);
        params.append('page', page);
        params.append('size', size);

        const res = await this._request(`${this.baseUrl}/claims?${params.toString()}`);
        if (!res.ok) {
            throw new Error(`Failed to fetch claims: HTTP ${res.status}`);
        }
        return res.json();
    },

    async getClaimById(id) {
        const res = await this._request(`${this.baseUrl}/claims/${id}`);
        if (!res.ok) {
            throw new Error(`Claim #${id} not found: HTTP ${res.status}`);
        }
        return res.json();
    },

    async getClaimByReference(reference) {
        const res = await this._request(`${this.baseUrl}/claims/reference/${encodeURIComponent(reference)}`);
        if (!res.ok) {
            throw new Error(`Claim ${reference} not found: HTTP ${res.status}`);
        }
        return res.json();
    },

    async createClaim(claimData) {
        const res = await this._request(`${this.baseUrl}/claims`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(claimData)
        });
        const data = await res.json();
        if (!res.ok) {
            const err = new Error(data.detail || 'Claim creation failed');
            err.status = res.status;
            err.fieldErrors = data.fieldErrors || [];
            throw err;
        }
        return data;
    },

    async startReview(id, reviewerName = 'Priya Sharma') {
        const res = await this._request(`${this.baseUrl}/claims/${id}/review`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reviewerName })
        });
        if (!res.ok) throw new Error(`Failed to start review: HTTP ${res.status}`);
        return res.json();
    },

    async approveClaim(id, remarks, deductible) {
        const res = await this._request(`${this.baseUrl}/claims/${id}/approve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ remarks, deductible })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Approval failed: HTTP ${res.status}`);
        return data;
    },

    async rejectClaim(id, reasonCode, remarks) {
        const res = await this._request(`${this.baseUrl}/claims/${id}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reasonCode, remarks })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Rejection failed: HTTP ${res.status}`);
        return data;
    },

    async requestMoreInfo(id, query, requestedDocTypes = [], reviewerName = 'Priya Sharma') {
        const res = await this._request(`${this.baseUrl}/claims/${id}/request-info`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query, requestedDocTypes, reviewerName })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Request info failed: HTTP ${res.status}`);
        return data;
    },

    async addDocument(id, documentMetadata) {
        const res = await this._request(`${this.baseUrl}/claims/${id}/documents`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(documentMetadata)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Upload metadata failed: HTTP ${res.status}`);
        return data;
    },

    async getClaimIndicators(id) {
        const res = await this._request(`${this.baseUrl}/claims/${id}/indicators`);
        if (!res.ok) {
            throw new Error(`Indicators for claim #${id} not found: HTTP ${res.status}`);
        }
        return res.json();
    },

    async getPendingReviewsCount() {
        const data = await this.getClaims({ status: 'UNDER_REVIEW', size: 1 });
        return data.page?.totalElements ?? data.totalElements ?? (data.content ? data.content.length : 0);
    },

    async updatePendingBadge() {
        const badge = document.getElementById('pending-badge');
        if (!badge) return;
        try {
            const count = await this.getPendingReviewsCount();
            badge.innerText = count;
            badge.title = `${count} pending review(s)`;
        } catch (err) {
            badge.innerText = '—';
            badge.title = 'Unable to fetch live pending count';
        }
    },

    async initUserSession() {
        // Fetch current user and render top header profile badge
        try {
            const user = await this.getCurrentUser();
            if (user && user.authenticated) {
                const userAvatar = document.getElementById('header-user-avatar');
                const userName = document.getElementById('header-user-name');
                const userRole = document.getElementById('header-user-role');
                const logoutBtn = document.getElementById('header-logout-btn');

                if (userAvatar) {
                    const initials = (user.fullName || user.username || 'PS')
                        .split(' ')
                        .map(n => n[0])
                        .join('')
                        .substring(0, 2)
                        .toUpperCase();
                    userAvatar.innerText = initials;
                }
                if (userName) userName.innerText = user.fullName || user.username;
                if (userRole) userRole.innerText = user.role || 'REVIEWER';

                if (logoutBtn) {
                    logoutBtn.classList.remove('hidden');
                    logoutBtn.addEventListener('click', (e) => {
                        e.preventDefault();
                        this.logout();
                    });
                }
            }
        } catch (e) {
            console.debug('Session check skipped:', e);
        }
    }
};

window.ClaimGuardAPI = API;

document.addEventListener('DOMContentLoaded', () => {
    // Check if not on login.html before running authenticated updates
    if (!window.location.pathname.includes('login.html')) {
        ClaimGuardAPI.initUserSession();
        ClaimGuardAPI.updatePendingBadge();
    }
});
