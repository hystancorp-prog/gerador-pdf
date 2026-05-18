/* masks.js — Hystan input masks */
(function () {
  'use strict';

  /* ── CPF / CNPJ ── */
  function maskCpfCnpj(input) {
    var cur = input.selectionStart;
    var oldLen = input.value.length;

    var v = input.value.replace(/\D/g, '');
    if (v.length <= 11) {
      v = v.substring(0, 11);
      if (v.length > 9) v = v.replace(/^(\d{3})(\d{3})(\d{3})(\d{1,2})$/, '$1.$2.$3-$4');
      else if (v.length > 6) v = v.replace(/^(\d{3})(\d{3})(\d{1,3})$/, '$1.$2.$3');
      else if (v.length > 3) v = v.replace(/^(\d{3})(\d{1,3})$/, '$1.$2');
    } else {
      v = v.substring(0, 14);
      if (v.length > 12) v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{1,2})$/, '$1.$2.$3/$4-$5');
      else if (v.length > 8) v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d{1,4})$/, '$1.$2.$3/$4');
      else if (v.length > 5) v = v.replace(/^(\d{2})(\d{3})(\d{1,3})$/, '$1.$2.$3');
      else if (v.length > 2) v = v.replace(/^(\d{2})(\d{1,3})$/, '$1.$2');
    }
    input.value = v;

    var diff = input.value.length - oldLen;
    var newPos = Math.max(0, Math.min(cur + diff, input.value.length));
    try { input.setSelectionRange(newPos, newPos); } catch (_) {}
  }

  /* ── TELEFONE ── */
  function maskTelefone(input) {
    var cur = input.selectionStart;
    var oldLen = input.value.length;

    var v = input.value.replace(/\D/g, '').substring(0, 11);
    if (v.length > 10) {
      v = v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
    } else if (v.length > 6) {
      v = v.replace(/^(\d{2})(\d{4,5})(\d{0,4})$/, function (_, a, b, c) {
        return '(' + a + ') ' + b + (c ? '-' + c : '');
      });
    } else if (v.length > 2) {
      v = v.replace(/^(\d{2})(\d+)$/, '($1) $2');
    } else if (v.length > 0) {
      v = v.replace(/^(\d+)$/, '($1');
    }
    input.value = v;

    var diff = input.value.length - oldLen;
    var newPos = Math.max(0, Math.min(cur + diff, input.value.length));
    try { input.setSelectionRange(newPos, newPos); } catch (_) {}
  }

  /* ── MOEDA (R$) ── */
  function maskMoeda(input) {
    var v = input.value.replace(/\D/g, '');
    if (!v || v === '0') { input.value = ''; return; }
    var num = parseInt(v, 10) / 100;
    var formatted = num.toFixed(2)
      .replace('.', ',')
      .replace(/(\d)(?=(\d{3})+,)/g, '$1.');
    input.value = 'R$ ' + formatted;
    var len = input.value.length;
    try { input.setSelectionRange(len, len); } catch (_) {}
  }

  /* ── CEP ── */
  function maskCep(input) {
    var cur = input.selectionStart;
    var oldLen = input.value.length;
    var v = input.value.replace(/\D/g, '').substring(0, 8);
    if (v.length > 5) v = v.replace(/^(\d{5})(\d{1,3})$/, '$1-$2');
    input.value = v;
    var diff = input.value.length - oldLen;
    var newPos = Math.max(0, Math.min(cur + diff, input.value.length));
    try { input.setSelectionRange(newPos, newPos); } catch (_) {}
  }

  /* ── PARSE HELPERS ── */
  function parseMoeda(v) {
    if (!v) return 0;
    return parseFloat(String(v).replace(/R\$\s*/g, '').replace(/\./g, '').replace(',', '.')) || 0;
  }

  /* ── APPLY HELPERS ── */
  function applyMaskOnInput(selector, maskFn, root) {
    var scope = root || document;
    scope.querySelectorAll(selector).forEach(function (el) {
      el.addEventListener('input', function () { maskFn(el); });
    });
  }

  /* Export */
  window.maskCpfCnpj   = maskCpfCnpj;
  window.maskTelefone  = maskTelefone;
  window.maskMoeda     = maskMoeda;
  window.maskCep       = maskCep;
  window.parseMoeda    = parseMoeda;
  window.applyMaskOnInput = applyMaskOnInput;
})();
