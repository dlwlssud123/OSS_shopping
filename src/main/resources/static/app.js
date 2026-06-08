// State Management
// 접속한 호스트를 자동으로 감지하여 API 주소로 설정 (상대 경로 및 VM 배포 시 자동 대응)
let apiBaseUrl = window.location.origin;
let products = [];
let customers = [];
let cart = {}; // productId -> quantity
let selectedCustomer = null;

// DOM Elements
const apiInput = document.getElementById('api-url');
const btnInitData = document.getElementById('btn-init-data');
const selectCustomer = document.getElementById('select-customer');
const badgeGrade = document.getElementById('badge-grade');
const productList = document.getElementById('product-list');
const cartItems = document.getElementById('cart-items');
const totalOriginal = document.getElementById('total-original');
const totalDiscount = document.getElementById('total-discount');
const totalFinal = document.getElementById('total-final');
const idempotencyKeyInput = document.getElementById('idempotency-key');
const btnRegenKey = document.getElementById('btn-regen-key');
const btnSubmitPayment = document.getElementById('btn-submit-payment');
const logConsole = document.getElementById('log-console');
const btnClearLogs = document.getElementById('btn-clear-logs');

// App Initialization
window.addEventListener('DOMContentLoaded', () => {
    generateIdempotencyKey();
    
    // 현재 접속 주소로 인풋 필드 초기화
    apiInput.value = apiBaseUrl;
    updateApiUrl();
    
    // Event Listeners
    apiInput.addEventListener('change', updateApiUrl);
    btnInitData.addEventListener('click', initDemoData);
    selectCustomer.addEventListener('change', handleCustomerChange);
    btnRegenKey.addEventListener('click', generateIdempotencyKey);
    btnSubmitPayment.addEventListener('click', submitPayment);
    btnClearLogs.addEventListener('click', clearLogs);

    // Initial load
    loadData();
});

function updateApiUrl() {
    apiBaseUrl = apiInput.value.trim() || window.location.origin;
    addLog(`서버 API 주소가 설정되었습니다: ${apiBaseUrl}`, 'info');
}

function generateIdempotencyKey() {
    const randomHex = () => Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1);
    const key = `KEY-${randomHex()}${randomHex()}-${randomHex()}-${randomHex()}`;
    idempotencyKeyInput.value = key;
    addLog(`새로운 멱등성 키가 생성되었습니다: ${key}`, 'info');
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

// Fetch Data from API
async function loadData() {
    try {
        await Promise.all([fetchCustomers(), fetchProducts()]);
    } catch (e) {
        addLog(`데이터 로드 중 오류가 발생했습니다. 백엔드가 구동 중인지 확인하세요. (${e.message})`, 'error');
    }
}

async function fetchProducts() {
    try {
        const res = await fetch(`${apiBaseUrl}/api/products`);
        if (!res.ok) throw new Error('API 응답 오류');
        products = await res.json();
        renderProducts();
    } catch (e) {
        throw new Error(`상품 조회 실패: ${e.message}`);
    }
}

async function fetchCustomers() {
    try {
        const res = await fetch(`${apiBaseUrl}/api/customers`);
        if (!res.ok) throw new Error('API 응답 오류');
        customers = await res.json();
        renderCustomers();
    } catch (e) {
        throw new Error(`회원 조회 실패: ${e.message}`);
    }
}

async function initDemoData() {
    addLog('데모 데이터 초기화를 요청합니다...', 'info');
    try {
        const res = await fetch(`${apiBaseUrl}/api/init`, { method: 'POST' });
        if (!res.ok) throw new Error('API 응답 오류');
        const result = await res.json();
        addLog(result.message, 'success');
        
        // 새로 데이터 로드
        await loadData();
    } catch (e) {
        addLog(`데모 데이터 초기화 실패: ${e.message}`, 'error');
    }
}

// Render Functions
function renderCustomers() {
    selectCustomer.innerHTML = '<option value="">-- 회원을 선택하세요 --</option>';
    
    customers.forEach(c => {
        const option = document.createElement('option');
        option.value = c.id;
        option.dataset.name = c.name;
        option.dataset.grade = c.grade;
        if (c.name === 'FAIL_USER') {
            option.textContent = `${c.name} [${c.grade}] (결제실패 테스트용)`;
        } else {
            option.textContent = `${c.name} [${c.grade}]`;
        }
        selectCustomer.appendChild(option);
    });
    
    if (selectedCustomer) {
        selectCustomer.value = selectedCustomer.id;
    }
}

function renderProducts() {
    productList.innerHTML = '';
    
    if (products.length === 0) {
        productList.innerHTML = '<p class="empty-cart-msg">등록된 상품이 없습니다. 데모 데이터 초기화를 실행하세요.</p>';
        return;
    }

    products.forEach(p => {
        const card = document.createElement('div');
        card.className = 'product-card';
        
        const isLowStock = p.stockQuantity <= 3;
        const stockHtml = isLowStock 
            ? `<span class="stock-value low">품절임박 (${p.stockQuantity}개)</span>` 
            : `<span class="stock-value">재고: ${p.stockQuantity}개</span>`;

        card.innerHTML = `
            <div class="product-info">
                <h3>${p.name}</h3>
                <div class="product-price">${formatWon(p.price)}</div>
                <div class="product-stock">
                    <span>ID: ${p.productId}</span>
                    ${stockHtml}
                </div>
            </div>
            <button class="btn-add-cart" ${p.stockQuantity === 0 ? 'disabled' : ''}>
                ${p.stockQuantity === 0 ? '품절' : '장바구니 담기'}
            </button>
        `;

        card.querySelector('.btn-add-cart').addEventListener('click', () => addToCart(p.productId));
        productList.appendChild(card);
    });
}

function handleCustomerChange() {
    const selectedOption = selectCustomer.options[selectCustomer.selectedIndex];
    if (!selectedOption || !selectedOption.value) {
        selectedCustomer = null;
        badgeGrade.className = 'badge';
        badgeGrade.textContent = '등급 미선택';
        addLog('회원 선택을 해제했습니다.', 'info');
        updateCartSummary();
        return;
    }

    selectedCustomer = {
        id: parseInt(selectedOption.value),
        name: selectedOption.dataset.name,
        grade: selectedOption.dataset.grade
    };

    badgeGrade.className = `badge ${selectedCustomer.grade.toLowerCase()}`;
    badgeGrade.textContent = selectedCustomer.grade;
    addLog(`${selectedCustomer.name} 회원이 선택되었습니다. (${selectedCustomer.grade} 등급)`, 'info');
    
    updateCartSummary();
}

// Cart Operations
function addToCart(productId) {
    const product = products.find(p => p.productId === productId);
    if (!product) return;

    if (product.stockQuantity <= 0) {
        addLog('상품 재고가 없어 장바구니에 담을 수 없습니다.', 'error');
        return;
    }

    const currentQty = cart[productId] || 0;
    if (currentQty >= product.stockQuantity) {
        addLog(`재고 수량을 초과해 담을 수 없습니다. (최대 ${product.stockQuantity}개)`, 'warn');
        return;
    }

    cart[productId] = currentQty + 1;
    addLog(`'${product.name}' 상품을 장바구니에 추가했습니다.`, 'info');
    renderCart();
}

function updateCartQty(productId, delta) {
    const product = products.find(p => p.productId === productId);
    if (!product) return;

    const currentQty = cart[productId] || 0;
    const newQty = currentQty + delta;

    if (newQty <= 0) {
        delete cart[productId];
        addLog(`'${product.name}' 상품이 장바구니에서 삭제되었습니다.`, 'info');
    } else if (newQty > product.stockQuantity) {
        addLog(`재고 수량을 초과해 설정할 수 없습니다.`, 'warn');
    } else {
        cart[productId] = newQty;
    }
    renderCart();
}

// Format currency
function formatWon(value) {
    return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(value);
}

function renderCart() {
    cartItems.innerHTML = '';
    const cartKeys = Object.keys(cart);

    if (cartKeys.length === 0) {
        cartItems.innerHTML = '<p class="empty-cart-msg">장바구니가 비어 있습니다.</p>';
        updateCartSummary();
        return;
    }

    cartKeys.forEach(pIdKey => {
        const pId = parseInt(pIdKey);
        const qty = cart[pId];
        const product = products.find(p => p.productId === pId);
        if (!product) return;

        const cartItemDiv = document.createElement('div');
        cartItemDiv.className = 'cart-item';
        cartItemDiv.innerHTML = `
            <div class="cart-item-name">${product.name}</div>
            <div class="cart-item-controls">
                <button class="cart-qty-btn decrease">-</button>
                <span class="cart-qty-val">${qty}</span>
                <button class="cart-qty-btn increase">+</button>
                <div class="cart-item-price">${formatWon(product.price * qty)}</div>
            </div>
        `;

        cartItemDiv.querySelector('.decrease').addEventListener('click', () => updateCartQty(pId, -1));
        cartItemDiv.querySelector('.increase').addEventListener('click', () => updateCartQty(pId, 1));
        cartItems.appendChild(cartItemDiv);
    });

    updateCartSummary();
}

function updateCartSummary() {
    let originalTotal = 0;
    
    Object.keys(cart).forEach(pIdKey => {
        const pId = parseInt(pIdKey);
        const qty = cart[pId];
        const product = products.find(p => p.productId === pId);
        if (product) {
            originalTotal += product.price * qty;
        }
    });

    // 로컬 예상 할인
    let discountTotal = 0;
    if (selectedCustomer && Object.keys(cart).length > 0) {
        const isVip = selectedCustomer.grade === 'VIP' || selectedCustomer.grade === 'VVIP';
        
        Object.keys(cart).forEach(pIdKey => {
            const pId = parseInt(pIdKey);
            const qty = cart[pId];
            const product = products.find(p => p.productId === pId);
            if (!product) return;

            const itemPrice = product.price * qty;
            let itemDiscount = 0;

            if (isVip) {
                itemDiscount += itemPrice * 0.10;
            }

            let remaining = itemPrice - itemDiscount;
            if (remaining > 0) {
                let fixDiscount = Math.min(1000, remaining);
                itemDiscount += fixDiscount;
            }

            if (itemDiscount > itemPrice) {
                itemDiscount = itemPrice;
            }

            discountTotal += itemDiscount;
        });
    }

    const finalTotal = Math.max(0, originalTotal - discountTotal);

    totalOriginal.textContent = formatWon(originalTotal);
    totalDiscount.textContent = `-${formatWon(discountTotal)}`;
    totalFinal.textContent = formatWon(finalTotal);
}

// Payment Submission
async function submitPayment() {
    if (!selectedCustomer) {
        addLog('결제를 진행하려면 먼저 회원을 선택해 주세요.', 'warn');
        alert('구매 회원을 선택하세요.');
        return;
    }

    const cartKeys = Object.keys(cart);
    if (cartKeys.length === 0) {
        addLog('장바구니가 비어 있습니다.', 'warn');
        alert('장바구니에 상품을 담아주세요.');
        return;
    }

    const idempotencyKey = idempotencyKeyInput.value.trim();
    if (!idempotencyKey) {
        addLog('멱등성 키가 누락되었습니다.', 'error');
        return;
    }

    const itemsDto = cartKeys.map(pIdKey => {
        const pId = parseInt(pIdKey);
        return {
            productId: pId,
            quantity: cart[pId]
        };
    });

    const requestPayload = {
        customerId: selectedCustomer.id,
        items: itemsDto,
        idempotencyKey: idempotencyKey
    };

    addLog('========================================', 'info');
    addLog('PG 결제 요청을 서버에 전송합니다...', 'info');

    btnSubmitPayment.disabled = true;
    btnSubmitPayment.textContent = '결제 처리 중...';

    try {
        const res = await fetch(`${apiBaseUrl}/api/orders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestPayload)
        });

        const result = await res.json();
        
        if (res.ok) {
            if (result.status === 'COMPLETE') {
                addLog(`결제 성공 (COMPLETE)`, 'success');
                addLog(`주문 ID: ${result.orderId}`, 'success');
                addLog(`최종 결제 금액: ${formatWon(result.finalAmount)}`, 'success');
                addLog(`할인 적용 금액: ${formatWon(result.discountAmount)}`, 'success');
                addLog(`PG 영수증 ID: ${result.receiptId}`, 'success');
                cart = {};
                renderCart();
            } else if (result.status === 'FAILED') {
                addLog(`결제 실패 (FAILED) - 보상 트랜잭션 정상 수행됨`, 'error');
                addLog(`주문 ID: ${result.orderId}`, 'error');
                addLog(`실패 사유: ${result.errorMessage}`, 'error');
                addLog(`차감된 재고가 원상태로 롤백되었습니다.`, 'warn');
            } else {
                addLog(`처리 오류: 상태=${result.status}, 메시지=${result.errorMessage}`, 'error');
            }
        } else {
            addLog(`서버 에러 (${res.status}): ${result.errorMessage || 'Unknown error'}`, 'error');
        }

        await fetchProducts();

    } catch (e) {
        addLog(`네트워크 또는 서버 에러 발생: ${e.message}`, 'error');
    } finally {
        btnSubmitPayment.disabled = false;
        btnSubmitPayment.textContent = '결제하기';
        addLog('========================================', 'info');
    }
}
