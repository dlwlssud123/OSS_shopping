// Auth Check
if (sessionStorage.getItem('adminAuthorized') !== 'true') {
    alert('관리자 인증이 필요합니다. 구매자 페이지에서 인증 후 접근하세요.');
    window.location.href = 'index.html';
}

// Admin State Management
let apiBaseUrl = 'http://localhost:8080';
let products = [];
let orders = [];

// DOM Elements
const apiInput = document.getElementById('api-url');
const btnInitData = document.getElementById('btn-init-data');

// Policy Elements
const policyRateActive = document.getElementById('policy-rate-active');
const policyRatePriority = document.getElementById('policy-rate-priority');
const policyRateExclusive = document.getElementById('policy-rate-exclusive');
const policyRateValue = document.getElementById('policy-rate-value');
const btnSaveRatePolicy = document.getElementById('btn-save-rate-policy');

const policyFixActive = document.getElementById('policy-fix-active');
const policyFixPriority = document.getElementById('policy-fix-priority');
const policyFixExclusive = document.getElementById('policy-fix-exclusive');
const policyFixValue = document.getElementById('policy-fix-value');
const btnSaveFixPolicy = document.getElementById('btn-save-fix-policy');

// Product Manager Elements
const prodName = document.getElementById('prod-name');
const prodPrice = document.getElementById('prod-price');
const prodStock = document.getElementById('prod-stock');
const btnAddProduct = document.getElementById('btn-add-product');
const tableProductsBody = document.querySelector('#table-products tbody');

// Orders Manager Elements
const tableOrdersBody = document.querySelector('#table-orders tbody');

// Logs Elements
const logConsole = document.getElementById('log-console');
const btnClearLogs = document.getElementById('btn-clear-logs');

// App Initialization
window.addEventListener('DOMContentLoaded', () => {
    updateApiUrl();

    // Event Listeners
    apiInput.addEventListener('change', updateApiUrl);
    btnInitData.addEventListener('click', initDemoData);
    
    // Policy Listeners
    btnSaveRatePolicy.addEventListener('click', () => savePolicy('RATE'));
    btnSaveFixPolicy.addEventListener('click', () => savePolicy('FIX'));
    
    // Product Manager Listeners
    btnAddProduct.addEventListener('click', addProduct);
    
    // Log Listeners
    btnClearLogs.addEventListener('click', clearLogs);

    // 구매자 모드로 나갈 때 세션 삭제
    const btnCustomerMode = document.querySelector('.btn-admin-link');
    if (btnCustomerMode) {
        btnCustomerMode.addEventListener('click', () => {
            sessionStorage.removeItem('adminAuthorized');
        });
    }

    // Initial Load
    loadAdminData();
});

function updateApiUrl() {
    apiBaseUrl = apiInput.value.trim() || 'http://localhost:8080';
    addLog(`서버 API 주소가 설정되었습니다: ${apiBaseUrl}`, 'info');
}

function addLog(message, type = 'info') {
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    const timestamp = new Date().toLocaleTimeString();
    entry.textContent = `[${timestamp}] ${message}`;
    logConsole.appendChild(entry);
    logConsole.scrollTop = logConsole.scrollHeight;
}

function clearLogs() {
    logConsole.innerHTML = '';
    addLog('로그가 비워졌습니다.', 'info');
}

// Load All Data
async function loadAdminData() {
    try {
        await Promise.all([
            fetchPolicies(),
            fetchProducts(),
            fetchOrders()
        ]);
        addLog('모든 데이터를 성공적으로 로드했습니다.', 'success');
    } catch (e) {
        addLog(`데이터 로드 중 오류 발생: ${e.message}`, 'error');
    }
}

// 1. Policy Resolver Settings (UC3)
async function fetchPolicies() {
    try {
        const res = await fetch(`${apiBaseUrl}/api/policies`);
        if (!res.ok) throw new Error('할인 정책 로드 실패');
        const policies = await res.json();
        
        // RATE 설정 셋팅
        const ratePol = policies.find(p => p.policyType === 'RATE') || {};
        policyRateActive.checked = ratePol.active || false;
        policyRatePriority.value = ratePol.priority || 1;
        policyRateExclusive.value = String(ratePol.exclusive || false);
        policyRateValue.value = ratePol.discountRate || 0.10;

        // FIX 설정 셋팅
        const fixPol = policies.find(p => p.policyType === 'FIX') || {};
        policyFixActive.checked = fixPol.active || false;
        policyFixPriority.value = fixPol.priority || 2;
        policyFixExclusive.value = String(fixPol.exclusive || false);
        policyFixValue.value = fixPol.discountAmount || 1000;
        
        addLog('런타임 할인 정책 설정을 가져왔습니다.', 'info');
    } catch (e) {
        addLog(`할인 정책 조회 에러: ${e.message}`, 'error');
    }
}

async function savePolicy(type) {
    addLog(`${type} 할인 정책 설정을 저장하는 중...`, 'info');
    
    let payload = {};
    if (type === 'RATE') {
        payload = {
            enabled: policyRateActive.checked,
            priority: parseInt(policyRatePriority.value),
            exclusive: policyRateExclusive.value === 'true',
            discountRate: parseFloat(policyRateValue.value)
        };
    } else {
        payload = {
            enabled: policyFixActive.checked,
            priority: parseInt(policyFixPriority.value),
            exclusive: policyFixExclusive.value === 'true',
            discountAmount: parseFloat(policyFixValue.value)
        };
    }

    try {
        const res = await fetch(`${apiBaseUrl}/api/policies/${type}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (!res.ok) throw new Error('정책 업데이트 실패');
        const updated = await res.json();
        addLog(`${type} 정책 저장 성공!`, 'success');
        await fetchPolicies();
    } catch (e) {
        addLog(`정책 저장 실패: ${e.message}`, 'error');
        alert(`정책 저장 실패: ${e.message}`);
    }
}

// 2. Product Manager Actions (UC1)
async function fetchProducts() {
    try {
        const res = await fetch(`${apiBaseUrl}/api/products`);
        if (!res.ok) throw new Error('상품 조회 실패');
        products = await res.json();
        renderProductsTable();
    } catch (e) {
        addLog(`상품 목록 갱신 에러: ${e.message}`, 'error');
        tableProductsBody.innerHTML = `<tr><td colspan="5" style="color:red;">로드 실패: ${e.message}</td></tr>`;
    }
}

function renderProductsTable() {
    tableProductsBody.innerHTML = '';
    if (products.length === 0) {
        tableProductsBody.innerHTML = `<tr><td colspan="5" style="text-align:center;">등록된 상품이 없습니다. 데모 데이터 초기화를 하거나 새로 추가해 주세요.</td></tr>`;
        return;
    }

    products.forEach(p => {
        const tr = document.createElement('tr');
        tr.id = `product-row-${p.productId}`;
        renderNormalRow(tr, p);
        tableProductsBody.appendChild(tr);
    });
}

function renderNormalRow(tr, p) {
    tr.innerHTML = `
        <td><strong>${p.productId}</strong></td>
        <td>${p.name}</td>
        <td>${formatWon(p.price)}</td>
        <td>${p.stockQuantity}개</td>
        <td>
            <button class="btn-mini btn-edit" data-id="${p.productId}">수정</button>
            <button class="btn-mini-secondary btn-delete" data-id="${p.productId}" style="margin-left:5px; border-color:var(--danger); color:var(--danger)">삭제</button>
        </td>
    `;
    
    tr.querySelector('.btn-edit').addEventListener('click', () => renderEditRow(tr, p));
    tr.querySelector('.btn-delete').addEventListener('click', () => deleteProduct(p.productId));
}

function renderEditRow(tr, p) {
    tr.innerHTML = `
        <td><strong>${p.productId}</strong></td>
        <td>
            <input type="text" class="styled-input inline-edit-name" value="${p.name}" style="padding: 4px 8px; font-size:13px; font-family:inherit; width: 140px;">
        </td>
        <td>
            <input type="number" class="styled-input inline-edit-price" value="${p.price}" style="padding: 4px 8px; font-size:13px; font-family:inherit; width: 100px;">
        </td>
        <td>
            <input type="number" class="styled-input inline-edit-stock" value="${p.stockQuantity}" style="padding: 4px 8px; font-size:13px; font-family:inherit; width: 70px;">
        </td>
        <td>
            <button class="btn-mini btn-save" style="border-color:var(--success); color:var(--success); background-color: transparent;">저장</button>
            <button class="btn-mini-secondary btn-cancel" style="margin-left:5px;">취소</button>
        </td>
    `;

    tr.querySelector('.btn-save').addEventListener('click', () => saveInlineProduct(tr, p.productId));
    tr.querySelector('.btn-cancel').addEventListener('click', () => renderNormalRow(tr, p));
}

async function saveInlineProduct(tr, id) {
    const newName = tr.querySelector('.inline-edit-name').value.trim();
    const newPriceVal = parseFloat(tr.querySelector('.inline-edit-price').value);
    const newStockVal = parseInt(tr.querySelector('.inline-edit-stock').value);

    if (!newName || isNaN(newPriceVal) || isNaN(newStockVal) || newPriceVal < 0 || newStockVal < 0) {
        alert('올바른 값을 입력해 주세요. (가격과 재고는 0 이상)');
        return;
    }

    addLog(`상품 ID ${id} 수정 요청 중...`, 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/products/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: newName,
                price: newPriceVal,
                stockQuantity: newStockVal
            })
        });

        if (!res.ok) throw new Error('상품 수정 실패');
        addLog(`상품 ID ${id} 수정 성공!`, 'success');
        await fetchProducts();
    } catch (e) {
        addLog(`상품 수정 실패: ${e.message}`, 'error');
        alert(`상품 수정 실패: ${e.message}`);
    }
}

async function addProduct() {
    const nameVal = prodName.value.trim();
    const priceVal = parseFloat(prodPrice.value);
    const stockVal = parseInt(prodStock.value);

    if (!nameVal || isNaN(priceVal) || isNaN(stockVal)) {
        alert('올바른 상품 정보를 입력해 주세요.');
        return;
    }

    addLog(`새 상품 '${nameVal}' 추가 요청 중...`, 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                name: nameVal,
                price: priceVal,
                stockQuantity: stockVal
            })
        });

        if (!res.ok) throw new Error('상품 추가 API 에러');
        addLog(`상품 '${nameVal}' 추가 성공!`, 'success');
        
        prodName.value = '';
        prodPrice.value = '';
        prodStock.value = '';
        
        await fetchProducts();
    } catch (e) {
        addLog(`상품 추가 실패: ${e.message}`, 'error');
        alert(`상품 추가 실패: ${e.message}`);
    }
}

async function deleteProduct(id) {
    if (!confirm(`상품 ID ${id}를 정말 삭제하시겠습니까?`)) return;

    addLog(`상품 ID ${id} 삭제 요청 중...`, 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/products/${id}`, {
            method: 'DELETE'
        });

        if (!res.ok) throw new Error('상품 삭제 실패');
        addLog(`상품 ID ${id} 삭제 완료`, 'success');
        await fetchProducts();
    } catch (e) {
        addLog(`상품 삭제 실패: ${e.message}`, 'error');
        alert(`상품 삭제 실패: ${e.message}`);
    }
}

// 3. Orders Manager & Cancel Actions (UC6)
async function fetchOrders() {
    try {
        const res = await fetch(`${apiBaseUrl}/api/orders`);
        if (!res.ok) throw new Error('주문 조회 실패');
        orders = await res.json();
        renderOrdersTable();
    } catch (e) {
        addLog(`주문 이력 갱신 에러: ${e.message}`, 'error');
        tableOrdersBody.innerHTML = `<tr><td colspan="5" style="color:red;">로드 실패: ${e.message}</td></tr>`;
    }
}

function renderOrdersTable() {
    tableOrdersBody.innerHTML = '';
    if (orders.length === 0) {
        tableOrdersBody.innerHTML = `<tr><td colspan="5" style="text-align:center;">주문 내역이 존재하지 않습니다.</td></tr>`;
        return;
    }

    orders.forEach(o => {
        const tr = document.createElement('tr');
        
        let statusBadge = `<span class="badge-status created">CREATED</span>`;
        if (o.status === 'COMPLETE') statusBadge = `<span class="badge-status complete">결제완료</span>`;
        if (o.status === 'FAILED') statusBadge = `<span class="badge-status failed">결제실패</span>`;
        if (o.status === 'CANCELED') statusBadge = `<span class="badge-status canceled">주문취소</span>`;
        if (o.status === 'PAYMENT_PENDING') statusBadge = `<span class="badge-status payment_pending">결제대기</span>`;

        const canCancel = o.status === 'COMPLETE';
        const cancelBtn = canCancel
            ? `<button class="btn-mini btn-cancel-order" data-id="${o.orderId}" style="border-color:var(--danger); color:var(--danger);">취소/반품</button>`
            : `<span style="color:var(--text-secondary); font-size:11px;">취소불가</span>`;

        tr.innerHTML = `
            <td><strong>#${o.orderId || '-'}</strong></td>
            <td>${statusBadge}</td>
            <td>
                <div><strong>${formatWon(o.finalAmount)}</strong></div>
                <div style="font-size:11px; color:var(--danger)">할인: -${formatWon(o.discountAmount)}</div>
            </td>
            <td><code style="font-size:11px;">${o.idempotencyKey || '-'}</code></td>
            <td>${cancelBtn}</td>
        `;

        if (canCancel) {
            tr.querySelector('.btn-cancel-order').addEventListener('click', () => cancelOrder(o.orderId));
        }

        tableOrdersBody.appendChild(tr);
    });
}

async function cancelOrder(id) {
    if (!confirm(`주문 #${id}을 정말 취소하고 재고를 복구하시겠습니까?`)) return;

    addLog(`주문 #${id} 취소 요청 중 (보상 트랜잭션)...`, 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/orders/${id}/cancel`, {
            method: 'POST'
        });

        const result = await res.json();
        
        if (res.ok && result.status === 'CANCELED') {
            addLog(`주문 #${id} 취소 성공! 재고가 다시 환원되었습니다.`, 'success');
            await Promise.all([
                fetchOrders(),
                fetchProducts()
            ]);
        } else {
            throw new Error(result.errorMessage || 'Unknown error');
        }
    } catch (e) {
        addLog(`주문 취소 실패: ${e.message}`, 'error');
        alert(`주문 취소 실패: ${e.message}`);
    }
}

// Init Demo Data (Shared Action)
async function initDemoData() {
    addLog('데모 데이터 초기화를 서버에 요청합니다...', 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/init`, { method: 'POST' });
        if (!res.ok) throw new Error('초기화 API 응답 오류');
        const result = await res.json();
        addLog(result.message, 'success');
        await loadAdminData();
    } catch (e) {
        addLog(`데모 데이터 초기화 실패: ${e.message}`, 'error');
    }
}

// Helper
function formatWon(value) {
    return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value);
}
