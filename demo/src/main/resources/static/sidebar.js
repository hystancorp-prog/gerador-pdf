/* sidebar.js — Hystan shared sidebar component */
(function () {
  'use strict';

  /* ── SVG ICONS ── */
  var IC = {
    dashboard: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="1.5" width="5" height="5" rx="1.2"/><rect x="9.5" y="1.5" width="5" height="5" rx="1.2"/><rect x="1.5" y="9.5" width="5" height="5" rx="1.2"/><rect x="9.5" y="9.5" width="5" height="5" rx="1.2"/></svg>',
    history:   '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="6.5"/><polyline points="8 4.5 8 8 10.5 10.5"/></svg>',
    planilha:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="1.5" width="13" height="13" rx="1.5"/><line x1="1.5" y1="5.5" x2="14.5" y2="5.5"/><line x1="1.5" y1="9.5" x2="14.5" y2="9.5"/><line x1="5.5" y1="5.5" x2="5.5" y2="14.5"/><line x1="9.5" y1="5.5" x2="9.5" y2="14.5"/></svg>',
    orcamento: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 1.5H4a1.5 1.5 0 0 0-1.5 1.5v10A1.5 1.5 0 0 0 4 14.5h8A1.5 1.5 0 0 0 13.5 13V5L10 1.5z"/><polyline points="10 1.5 10 5.5 13.5 5.5"/><line x1="5.5" y1="8.5" x2="10.5" y2="8.5"/><line x1="5.5" y1="11" x2="8.5" y2="11"/></svg>',
    recibo:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 1.5v13l2.5-1.5 2.5 1.5 2.5-1.5L13 14.5V1.5"/><line x1="6" y1="6" x2="10" y2="6"/><line x1="6" y1="9" x2="10" y2="9"/></svg>',
    settings:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="8" cy="8" r="2.4"/><path d="M8 1.5v1.2M8 13.3v1.2M1.5 8h1.2M13.3 8h1.2M3.4 3.4l.85.85M11.75 11.75l.85.85M3.4 12.6l.85-.85M11.75 4.25l.85-.85"/></svg>',
    empresas:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1.5" y="5" width="13" height="9.5" rx="1"/><path d="M5 5V3.5A1.5 1.5 0 0 1 6.5 2h3A1.5 1.5 0 0 1 11 3.5V5"/><line x1="8" y1="8" x2="8" y2="11"/><line x1="6" y1="9.5" x2="10" y2="9.5"/></svg>',
    planos:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2H3a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1z"/><path d="M8 5v6M5.5 7.5 8 5l2.5 2.5"/></svg>',
    logout:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 14H3a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1h3"/><polyline points="10.5 11 14 8 10.5 5"/><line x1="14" y1="8" x2="6" y2="8"/></svg>'
  };

  /* ── HELPERS ── */
  function spaItem(key, navigateTo, label, icon, activePage) {
    var active = activePage === key ? ' is-active' : '';
    return '<button class="nav-item' + active + '" data-spa="' + navigateTo + '">' +
      '<span class="nav-icon">' + icon + '</span><span>' + label + '</span></button>';
  }

  function linkItem(key, href, label, icon, activePage) {
    var active = activePage === key ? ' is-active' : '';
    return '<a class="nav-item' + active + '" href="' + href + '">' +
      '<span class="nav-icon">' + icon + '</span><span>' + label + '</span></a>';
  }

  /* ── OPEN / CLOSE ── */
  function openSidebar() {
    var s = document.getElementById('sidebar');
    var o = document.getElementById('sidebar-overlay');
    if (s) s.classList.add('is-open');
    if (o) o.classList.add('is-visible');
  }
  function closeSidebar() {
    var s = document.getElementById('sidebar');
    var o = document.getElementById('sidebar-overlay');
    if (s) s.classList.remove('is-open');
    if (o) o.classList.remove('is-visible');
  }

  /* ── BUILD ── */
  function buildSidebar(activePage) {
    var container = document.getElementById('sidebar-container');
    if (!container) return;

    /* inject overlay as first child of body if not present */
    if (!document.getElementById('sidebar-overlay')) {
      var ov = document.createElement('div');
      ov.id = 'sidebar-overlay';
      ov.className = 'sidebar-overlay';
      document.body.insertBefore(ov, document.body.firstChild);
    }

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
          linkItem('planos',   '/planos.html',   'Planos',   IC.planos,   activePage) +
          '<button class="nav-item" id="sidebar-logout-btn">' +
            '<span class="nav-icon">' + IC.logout + '</span><span>Sair</span>' +
          '</button>' +
          '<div class="nav-spacer"></div>' +
        '</nav>' +
        '<div class="sidebar-plan">' +
          '<div class="sidebar-plan-label">Seu plano</div>' +
          '<div class="sidebar-plan-name" id="sidebar-plan-name">' + planName + '</div>' +
          '<div class="sidebar-plan-sub" id="sidebar-plan-sub">' + planSub + '</div>' +
        '</div>' +
      '</aside>';

    /* wire SPA nav buttons */
    container.querySelectorAll('[data-spa]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var page = btn.getAttribute('data-spa');
        if (typeof window._navigate === 'function') {
          window._navigate(page);
        } else {
          window.location.href = '/dashboard.html';
        }
        closeSidebar();
      });
    });

    /* wire logout */
    var logoutBtn = document.getElementById('sidebar-logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', function () {
        if (typeof window._doSignOut === 'function') window._doSignOut();
      });
    }

    /* wire overlay */
    var overlay = document.getElementById('sidebar-overlay');
    if (overlay) overlay.addEventListener('click', closeSidebar);

    /* wire hamburger */
    var hamburger = document.getElementById('btn-hamburger');
    if (hamburger) hamburger.addEventListener('click', openSidebar);

    /* ESC closes sidebar */
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') closeSidebar();
    });
  }

  window.buildSidebar   = buildSidebar;
  window._openSidebar   = openSidebar;
  window._closeSidebar  = closeSidebar;
})();
