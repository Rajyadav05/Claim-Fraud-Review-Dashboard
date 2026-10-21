/**
 * ClaimGuard REST API Client
 * Connects the frontend UI to Spring Boot endpoints.
 */
const API = {
    baseUrl: '/api',

    async getHealth() {
        const res = await fetch(`${this.baseUrl}/health`);
        return res.json();
    },

    async getClaims({ status, risk, search, page = 0, size = 15 } = {}) {
        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (risk) params.append('risk', risk);
        if (search) params.append('search', search);
        params.append('page', page);
        params.append('size', size);

        const res = await fetch(`${this.baseUrl}/claims?${params.toString()}`);
        if (!res.ok) {
            throw new Error(`Failed to fetch claims: HTTP ${res.status}`);
        }
        return res.json();
    },

    async getClaimById(id) {
        const res = await fetch(`${this.baseUrl}/claims/${id}`);
        if (!res.ok) {
            throw new Error(`Claim #${id} not found: HTTP ${res.status}`);
        }
        return res.json();
    },

    async getClaimByReference(reference) {
        const res = await fetch(`${this.baseUrl}/claims/reference/${encodeURIComponent(reference)}`);
        if (!res.ok) {
            throw new Error(`Claim ${reference} not found: HTTP ${res.status}`);
        }
        return res.json();
    },

    async createClaim(claimData) {
        const res = await fetch(`${this.baseUrl}/claims`, {
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
        const res = await fetch(`${this.baseUrl}/claims/${id}/review`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reviewerName })
        });
        if (!res.ok) throw new Error(`Failed to start review: HTTP ${res.status}`);
        return res.json();
    },

    async approveClaim(id, remarks, deductible) {
        const res = await fetch(`${this.baseUrl}/claims/${id}/approve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ remarks, deductible })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Approval failed: HTTP ${res.status}`);
        return data;
    },

    async rejectClaim(id, reasonCode, remarks) {
        const res = await fetch(`${this.baseUrl}/claims/${id}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reasonCode, remarks })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Rejection failed: HTTP ${res.status}`);
        return data;
    },

    async requestMoreInfo(id, query, requestedDocTypes = [], reviewerName = 'Priya Sharma') {
        const res = await fetch(`${this.baseUrl}/claims/${id}/request-info`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query, requestedDocTypes, reviewerName })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Request info failed: HTTP ${res.status}`);
        return data;
    },

    async addDocument(id, documentMetadata) {
        const res = await fetch(`${this.baseUrl}/claims/${id}/documents`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(documentMetadata)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.detail || `Upload metadata failed: HTTP ${res.status}`);
        return data;
    }
};

window.ClaimGuardAPI = API;
