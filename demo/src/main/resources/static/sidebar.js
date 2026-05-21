/* sidebar.js — Hystan shared sidebar (self-contained) */
(function () {
  'use strict';

  /* ── CSS injected once ── */
  var CSS =
    '.sidebar-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.48);backdrop-filter:blur(3px);z-index:299}' +
    '.sidebar-overlay.is-visible{display:block}' +
    '.sidebar{width:240px;background:#111111;position:fixed;top:0;left:0;bottom:0;z-index:300;display:flex;flex-direction:column;transition:transform 260ms cubic-bezier(0.4,0,0.2,1)}' +
    '.sidebar-logo{padding:20px 20px 18px;border-bottom:1px solid rgba(255,255,255,0.08);flex-shrink:0}' +
    '.sidebar-nav{flex:1;padding:12px 10px;display:flex;flex-direction:column;gap:1px;overflow-y:auto}' +
    '.nav-section-label{font-size:10px;font-weight:600;letter-spacing:0.1em;text-transform:uppercase;color:rgba(255,255,255,0.25);padding:12px 8px 6px;user-select:none}' +
    '.nav-item{position:relative;display:flex;align-items:center;gap:10px;padding:9px 12px;border-width:1px 1px 1px 2px;border-style:solid;border-color:transparent;border-radius:8px;font-family:"Syne",sans-serif;font-size:13.5px;font-weight:500;color:rgba(255,255,255,0.52);background:transparent;cursor:pointer;width:100%;text-align:left;text-decoration:none;transition:background 150ms,color 150ms,border-color 150ms;outline:none;overflow:hidden}' +
    '.nav-item::before{content:"";position:absolute;inset:0;border-radius:8px;background:linear-gradient(120deg,rgba(255,255,255,0.03) 0%,rgba(255,255,255,0.12) 20%,rgba(200,200,200,0.08) 40%,rgba(255,255,255,0.15) 60%,rgba(180,180,180,0.06) 80%,rgba(255,255,255,0.04) 100%);background-size:300% 300%;opacity:0;z-index:0;pointer-events:none;transition:opacity 150ms}' +
    '.nav-item>span{position:relative;z-index:1}' +
    '.nav-item:hover{background:rgba(255,255,255,0.04);color:rgba(255,255,255,0.9)}' +
    '.nav-item.is-active{background:transparent;color:#ffffff;font-weight:600;border-color:rgba(255,255,255,0.12);border-left-color:rgba(255,255,255,0.7)}' +
    '.nav-item.is-active::before{opacity:1;animation:_sb_iri 4s ease infinite}' +
    '@keyframes _sb_iri{0%{background-position:0% 50%}50%{background-position:100% 50%}100%{background-position:0% 50%}}' +
    '.nav-icon{width:16px;height:16px;flex-shrink:0;display:flex;align-items:center;justify-content:center}' +
    '.nav-spacer{flex:1;min-height:16px}' +
    '.sidebar-plan{margin:12px;padding:14px 16px;border:1px solid rgba(255,255,255,0.08);border-radius:12px;background:rgba(255,255,255,0.03);flex-shrink:0}' +
    '.sidebar-plan-label{font-size:10px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;color:rgba(255,255,255,0.28);margin-bottom:4px}' +
    '.sidebar-plan-name{font-family:"Syne",sans-serif;font-size:14px;font-weight:700;color:#ffffff;margin-bottom:2px}' +
    '.sidebar-plan-sub{font-size:11px;color:rgba(255,255,255,0.38)}' +
    '@media(max-width:768px){' +
      '#sidebar{transform:translateX(-100%)!important;transition:transform 0.25s ease}' +
      '#sidebar.is-open,#sidebar.open{transform:translateX(0)!important}' +
      '#sidebar-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:199}' +
      '#sidebar-overlay.show{display:block}' +
    '}' +
    /* logout modal */
    '._sb_modal_ov{display:none;position:fixed;inset:0;background:rgba(0,0,0,0.38);backdrop-filter:blur(8px);z-index:9999;align-items:center;justify-content:center}' +
    '._sb_modal_ov.is-visible{display:flex}' +
    '._sb_modal{background:var(--surface,#ffffff);color:var(--text,#1a1a1a);border:1px solid var(--border,#e0e0dd);border-radius:16px;padding:32px;max-width:380px;width:calc(100% - 32px);box-shadow:0 24px 64px rgba(0,0,0,0.18)}' +
    '._sb_modal_title{font-family:"Syne",sans-serif;font-size:20px;font-weight:800;color:inherit;margin-bottom:8px;letter-spacing:-0.02em}' +
    '._sb_modal_body{font-size:14px;color:inherit;opacity:0.75;line-height:1.65;margin-bottom:24px}' +
    '._sb_modal_actions{display:flex;gap:10px}' +
    '._sb_modal_actions button{flex:1;padding:12px;border-radius:10px;font-size:14px;font-weight:600;cursor:pointer;font-family:"DM Sans",sans-serif;border:none;transition:opacity 150ms}' +
    '._sb_modal_actions button:hover{opacity:0.84}' +
    '#_sb_btn_cancel{background:var(--surface-2,#f5f5f3);color:var(--text,#1a1a1a);border:1px solid var(--border,#e0e0dd)}' +
    '#_sb_btn_confirm{background:#dc2626;color:#ffffff}' +
    /* loader dots */
    '.loader-dot{width:6px;height:6px;border-radius:50%;animation:loaderBounce 1.2s ease-in-out infinite}' +
    '.loader-dot:nth-child(2){animation-delay:0.2s}' +
    '.loader-dot:nth-child(3){animation-delay:0.4s}' +
    '@keyframes loaderBounce{0%,80%,100%{transform:scale(0.6);opacity:0.3}40%{transform:scale(1);opacity:1}}';

  function injectCSS() {
    if (document.getElementById('_sidebar_css')) return;
    var s = document.createElement('style');
    s.id = '_sidebar_css';
    s.textContent = CSS;
    document.head.appendChild(s);
  }

  /* ── THEME DETECTION ── */
  function isDarkTheme() {
    var t = document.documentElement.getAttribute('data-theme') || 'system';
    if (t === 'dark')  return true;
    if (t === 'light') return false;
    return !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
  }

  /* ── ICONS ── */
  var IC = {
    dashboard: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="1.5" width="5" height="5" rx="1.2"/><rect x="9.5" y="1.5" width="5" height="5" rx="1.2"/><rect x="1.5" y="9.5" width="5" height="5" rx="1.2"/><rect x="9.5" y="9.5" width="5" height="5" rx="1.2"/></svg>',
    history:   '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="6.5"/><polyline points="8 4.5 8 8 10.5 10.5"/></svg>',
    planilha:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="1.5" width="13" height="13" rx="1.5"/><line x1="1.5" y1="5.5" x2="14.5" y2="5.5"/><line x1="1.5" y1="9.5" x2="14.5" y2="9.5"/><line x1="5.5" y1="5.5" x2="5.5" y2="14.5"/><line x1="9.5" y1="5.5" x2="9.5" y2="14.5"/></svg>',
    orcamento: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 1.5H4a1.5 1.5 0 0 0-1.5 1.5v10A1.5 1.5 0 0 0 4 14.5h8A1.5 1.5 0 0 0 13.5 13V5L10 1.5z"/><polyline points="10 1.5 10 5.5 13.5 5.5"/><line x1="5.5" y1="8.5" x2="10.5" y2="8.5"/><line x1="5.5" y1="11" x2="8.5" y2="11"/></svg>',
    recibo:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 1.5v13l2.5-1.5 2.5 1.5 2.5-1.5L13 14.5V1.5"/><line x1="6" y1="6" x2="10" y2="6"/><line x1="6" y1="9" x2="10" y2="9"/></svg>',
    settings:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="2.4"/><path d="M8 1.5v1.2M8 13.3v1.2M1.5 8h1.2M13.3 8h1.2M3.4 3.4l.85.85M11.75 11.75l.85.85M3.4 12.6l.85-.85M11.75 4.25l.85-.85"/></svg>',
    empresas:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="5" width="13" height="9.5" rx="1"/><path d="M5 5V3.5A1.5 1.5 0 0 1 6.5 2h3A1.5 1.5 0 0 1 11 3.5V5"/><line x1="8" y1="8" x2="8" y2="11"/><line x1="6" y1="9.5" x2="10" y2="9.5"/></svg>',
    logout:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 14H3a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1h3"/><polyline points="10.5 11 14 8 10.5 5"/><line x1="14" y1="8" x2="6" y2="8"/></svg>'
  };

  /* ── HTML HELPERS ── */
  function spaItem(key, navigateTo, label, icon, activePage) {
    var cls = 'nav-item' + (activePage === key ? ' is-active' : '');
    return '<button class="' + cls + '" data-spa="' + navigateTo + '">' +
      '<span class="nav-icon">' + icon + '</span><span>' + label + '</span></button>';
  }
  function linkItem(key, href, label, icon, activePage) {
    var cls = 'nav-item' + (activePage === key ? ' is-active' : '');
    return '<a class="' + cls + '" href="' + href + '">' +
      '<span class="nav-icon">' + icon + '</span><span>' + label + '</span></a>';
  }

  /* ── OPEN / CLOSE SIDEBAR ── */
  function openSidebar() {
    var s = document.getElementById('sidebar');
    var o = document.getElementById('sidebar-overlay');
    if (s) { s.classList.add('is-open'); s.classList.add('open'); }
    if (o) { o.classList.add('is-visible'); o.classList.add('show'); }
  }
  function closeSidebar() {
    var s = document.getElementById('sidebar');
    var o = document.getElementById('sidebar-overlay');
    if (s) { s.classList.remove('is-open'); s.classList.remove('open'); }
    if (o) { o.classList.remove('is-visible'); o.classList.remove('show'); }
  }

  /* ── ATTACH HAMBURGER EVENTS ── */
  function attachHamburger() {
    const btn     = document.getElementById('btn-menu');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebar-overlay');

    if (!btn) {
      setTimeout(attachHamburger, 100);
      return;
    }

    /* Remove listeners antigos para não duplicar */
    const newBtn = btn.cloneNode(true);
    btn.parentNode.replaceChild(newBtn, btn);

    newBtn.addEventListener('click', function (e) {
      e.stopPropagation();
      if (sidebar) sidebar.classList.toggle('open');
      if (overlay) overlay.classList.toggle('show');
    });

    if (overlay) {
      overlay.addEventListener('click', function () {
        if (sidebar) sidebar.classList.remove('open');
        if (overlay) overlay.classList.remove('show');
      });
    }

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        if (sidebar) sidebar.classList.remove('open');
        if (overlay) overlay.classList.remove('show');
      }
    });
  }

  /* ── SET ACTIVE ITEM (SPA navigation) ── */
  var SPA_SELECTOR_MAP = {
    'dashboard':     '[data-spa="dashboard"]',
    'historico':     '[data-spa="history"]',
    'configuracoes': '[data-spa="settings"]',
    'planilha':      '[href="/planilha.html"]',
    'orcamento':     '[href="/orcamento.html"]',
    'recibo':        '[href="/recibo.html"]',
    'empresas':      '[href="/empresas.html"]'
  };

  function setSidebarActive(activePage) {
    var sidebar = document.getElementById('sidebar');
    if (!sidebar) return;
    sidebar.querySelectorAll('.nav-item').forEach(function (el) {
      el.classList.remove('is-active');
    });
    var sel = SPA_SELECTOR_MAP[activePage];
    if (sel) {
      var target = sidebar.querySelector('.nav-item' + sel);
      if (target) target.classList.add('is-active');
    }
  }

  /* ── LOGOUT MODAL ── */
  function showLogoutModal() {
    if (document.getElementById('logout-modal')) return;
    var modal = document.createElement('div');
    modal.id = 'logout-modal';
    modal.style.cssText = 'position:fixed;inset:0;z-index:99999;background:rgba(0,0,0,0.6);backdrop-filter:blur(8px);display:flex;align-items:center;justify-content:center;';
    modal.innerHTML =
      '<div style="background:var(--surface,#ffffff);color:var(--text,#1a1a1a);border:1px solid var(--border-strong,#c8c8c4);border-radius:16px;padding:32px;max-width:400px;width:90%;box-shadow:0 24px 60px rgba(0,0,0,0.3);">' +
        '<h3 style="font-family:Syne,sans-serif;font-size:20px;font-weight:800;margin:0 0 8px;color:var(--text,#1a1a1a);">Sair da conta?</h3>' +
        '<p style="font-size:14px;color:var(--text-muted,#888);margin:0 0 24px;line-height:1.5;">Tem certeza que deseja encerrar sua sessão neste dispositivo?</p>' +
        '<div style="display:flex;gap:10px;">' +
          '<button id="logout-cancel" style="flex:1;padding:12px;border-radius:8px;border:1px solid var(--border,#e0e0dd);background:var(--surface-2,#f5f5f3);color:var(--text,#1a1a1a);font-size:14px;font-weight:600;cursor:pointer;font-family:\'DM Sans\',sans-serif;">Cancelar</button>' +
          '<button id="logout-confirm" style="flex:1;padding:12px;border-radius:8px;border:none;background:#dc2626;color:white;font-size:14px;font-weight:600;cursor:pointer;font-family:\'DM Sans\',sans-serif;">Sair</button>' +
        '</div>' +
      '</div>';
    document.body.appendChild(modal);
    document.getElementById('logout-cancel').onclick = function () { modal.remove(); };
    document.getElementById('logout-confirm').onclick = function () {
      modal.remove();
      if (typeof window.logout === 'function') window.logout();
    };
    modal.addEventListener('click', function (e) { if (e.target === modal) modal.remove(); });
    document.addEventListener('keydown', function escHandler(e) {
      if (e.key === 'Escape') { modal.remove(); document.removeEventListener('keydown', escHandler); }
    });
  }

  /* ── LOADER ── */
  function injectLoader() {
    if (document.getElementById('hystan-loader')) return;
    var dark    = isDarkTheme();
    var bg      = dark ? '#0a0a0a' : '#f5f5f3';
    var logo    = dark ? '/hystan_white.png' : '/hystan.png';
    var dotClr  = dark ? 'rgba(255,255,255,0.5)' : 'rgba(0,0,0,0.35)';
    var dot     = '<div class="loader-dot" style="background:' + dotClr + '"></div>';
    var loader  = document.createElement('div');
    loader.id   = 'hystan-loader';
    loader.style.cssText =
      'position:fixed;inset:0;z-index:99999;background:' + bg + ';' +
      'display:flex;flex-direction:column;align-items:center;justify-content:center;' +
      'gap:24px;transition:opacity 0.3s ease;';
    loader.innerHTML =
      '<img src="' + logo + '" style="height:32px;opacity:0.9;" alt="Hystan">' +
      '<div style="display:flex;gap:8px;">' + dot + dot + dot + '</div>';
    document.body.appendChild(loader);
  }

  function hideLoader() {
    var loader = document.getElementById('hystan-loader');
    if (!loader) return;
    loader.style.pointerEvents = 'none'; /* allow clicks through while fading */
    loader.style.opacity = '0';
    setTimeout(function () {
      var l = document.getElementById('hystan-loader');
      if (l) l.remove();
    }, 300);
  }

  /* ── BUILD SIDEBAR ── */
  function buildSidebar(activePage) {
    injectCSS();

    var container = document.getElementById('sidebar-container');
    if (!container) return;

    /* inject overlay once */
    if (!document.getElementById('sidebar-overlay')) {
      var ov = document.createElement('div');
      ov.id = 'sidebar-overlay';
      ov.className = 'sidebar-overlay';
      document.body.insertBefore(ov, document.body.firstChild);
    }

    injectLoader();

    var plano    = localStorage.getItem('hystan_plano') || '';
    var planName = plano === 'ativo' ? 'Plano Business' : plano === 'trial' ? 'Trial' : '—';
    var planSub  = plano === 'ativo' ? 'Relatórios ilimitados' : plano === 'trial' ? 'Período de teste' : '—';

    container.innerHTML =
      '<aside class="sidebar" id="sidebar">' +
        '<div class="sidebar-logo">' +
          '<img src="/hystan_white.png" alt="Hystan" style="height:28px;width:auto;display:block;">' +
        '</div>' +
        '<nav class="sidebar-nav">' +
          '<div class="nav-section-label">Principal</div>' +
          spaItem('dashboard',    'dashboard', 'Dashboard', IC.dashboard, activePage) +
          spaItem('historico',    'history',   'Histórico', IC.history,   activePage) +
          '<div class="nav-section-label" style="margin-top:8px;">Ferramentas</div>' +
          linkItem('planilha',  '/planilha.html',  'Planilha → PDF', IC.planilha,  activePage) +
          linkItem('orcamento', '/orcamento.html', 'Orçamento',      IC.orcamento, activePage) +
          linkItem('recibo',    '/recibo.html',    'Recibo',          IC.recibo,    activePage) +
          '<div class="nav-section-label" style="margin-top:8px;">Conta</div>' +
          spaItem('configuracoes', 'settings', 'Configurações', IC.settings, activePage) +
          linkItem('empresas', '/empresas.html', 'Empresas', IC.empresas, activePage) +
          '<button class="nav-item" id="sidebar-logout-btn">' +
            '<span class="nav-icon">' + IC.logout + '</span><span>Sair</span>' +
          '</button>' +
          '<div class="nav-spacer"></div>' +
        '</nav>' +
        '<div class="sidebar-plan">' +
          '<div class="sidebar-plan-label">Seu plano</div>' +
          '<div class="sidebar-plan-name" id="sidebar-plan-name">' + planName + '</div>' +
          '<div class="sidebar-plan-sub"  id="sidebar-plan-sub">'  + planSub  + '</div>' +
        '</div>' +
      '</aside>';

    /* SPA nav buttons */
    container.querySelectorAll('[data-spa]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var target = btn.getAttribute('data-spa');
        if (typeof window._navigate === 'function') {
          window._navigate(target);
        } else {
          window.location.href = '/dashboard.html';
        }
        closeSidebar();
      });
    });

    /* logout → show confirmation modal */
    var logoutBtn = document.getElementById('sidebar-logout-btn');
    if (logoutBtn) logoutBtn.addEventListener('click', showLogoutModal);

    /* overlay click */
    var overlay = document.getElementById('sidebar-overlay');
    if (overlay) overlay.addEventListener('click', closeSidebar);

    /* hamburger */
    var hamburger = document.getElementById('btn-hamburger');
    if (hamburger) hamburger.addEventListener('click', openSidebar);

    /* ESC */
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { closeSidebar(); }
    });

    attachHamburger();
  }

  /* ── BOOTSTRAP HAMBURGER ON LOAD ── */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', attachHamburger);
  } else {
    attachHamburger();
  }

  /* ── EXPORTS ── */
  window.buildSidebar      = buildSidebar;
  window._setSidebarActive = setSidebarActive;
  window._openSidebar      = openSidebar;
  window._closeSidebar     = closeSidebar;
  window._hideLoader       = hideLoader;
})();
