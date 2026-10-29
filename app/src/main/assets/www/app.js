/* Phần riêng của ứng dụng Android: xếp gọn thanh điều khiển và chọn giao diện sáng / tối.
   Trong app, window.KmbApp (MainActivity) giữ lựa chọn giao diện và đổi màu thanh trạng thái theo;
   mở trang trong trình duyệt thì lựa chọn lưu ở localStorage. */
(function () {
  'use strict';
  var app = window.KmbApp;
  var KEY = 'kmb-theme';
  var $ = function (id) { return document.getElementById(id); };

  /* ---------- Thanh điều khiển: ba dòng ---------- */
  // Bọc phần chữ dài trong .kmb-long để màn hẹp ẩn đi, còn lại phần ngắn.
  function abbreviate(el, short) {
    var text = el && el.textContent;
    if (!text || text.indexOf(short) !== 0 || text === short) return;
    var rest = document.createElement('span');
    rest.className = 'kmb-long';
    rest.textContent = text.slice(short.length);
    el.textContent = short;
    el.appendChild(rest);
    el.title = text;
  }

  (function () {
    var form = $('ctrl');
    var rows = [
      ['ctl-time', ['fDate', 'fTime', 'bNow']],
      ['ctl-opts', ['fTz', 'fTy', 'segHan']],
      ['ctl-mode', ['segPhai', 'segCuc', 'segSu']]
    ];
    if (!form) return;
    var cells = rows.map(function (r) {
      return r[1].map(function (id) { var el = $(id); return el && el.closest('.field'); });
    });
    // Trang đã đổi khác thì để nguyên bố cục gốc.
    if (cells.some(function (c) { return c.indexOf(null) >= 0; })) return;

    rows.forEach(function (r, i) {
      var row = document.createElement('div');
      row.className = 'ctl-row ' + r[0];
      cells[i].forEach(function (f) { row.appendChild(f); });
      form.appendChild(row);
    });
    // Ô nào trang thêm về sau mà chưa có chỗ thì xuống một dòng riêng ở cuối.
    var rest = [].filter.call(form.children, function (c) { return c.classList.contains('field'); });
    if (rest.length) {
      var row = document.createElement('div');
      row.className = 'ctl-row';
      rest.forEach(function (f) { row.appendChild(f); });
      form.appendChild(row);
    }
    form.classList.add('ctl');
    abbreviate($('segSu').closest('.field').querySelector('.lab'), 'Trực sử');
    abbreviate($('segHan').querySelector('[data-v="1"]'), 'Hiện');

    // Ô chọn không cắt chữ được gọn, nên màn hẹp dùng nhãn ngắn: «UTC+7 · Việt Nam» → «UTC+7»,
    // «Tý muộn · 23:00» → «23:00» (nhãn «Giao ngày» đã nói rõ nghĩa).
    var narrow = window.matchMedia('(max-width:640px)');
    var short = [['fTz', function (t) { return t.split(' · ')[0]; }], ['fTy', function (t) { return t.split(' · ').pop(); }]];
    function labels() {
      short.forEach(function (s) {
        [].forEach.call($(s[0]).options, function (o) {
          if (!o.dataset.full) o.dataset.full = o.textContent;
          o.textContent = narrow.matches ? s[1](o.dataset.full) : o.dataset.full;
        });
      });
    }
    labels();
    if (narrow.addEventListener) narrow.addEventListener('change', labels);
  })();

  /* ---------- Giao diện ---------- */
  var MODES = { auto: 'Theo máy', light: 'Sáng', dark: 'Tối' };
  var ICONS = {
    auto: '<circle cx="12" cy="12" r="8.5"/><path class="fill" d="M12 3.5a8.5 8.5 0 0 1 0 17z"/>',
    light: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.3 5.3l1.6 1.6M17.1 17.1l1.6 1.6M5.3 18.7l1.6-1.6M17.1 6.9l1.6-1.6"/>',
    dark: '<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z"/>'
  };
  function icon(m) { return '<svg viewBox="0 0 24 24" aria-hidden="true">' + ICONS[m] + '</svg>'; }

  function getMode() {
    var m;
    try { m = app ? app.getThemeMode() : localStorage.getItem(KEY); } catch (e) { }
    return Object.prototype.hasOwnProperty.call(MODES, m) ? m : 'auto';
  }

  // Trong app, «Theo máy» cũng đặt data-theme rõ ràng theo chế độ tối của Android,
  // để trang và thanh hệ thống (do MainActivity tô) luôn cùng một màu.
  function apply(m) {
    var t = m === 'auto' ? (app ? (app.isSystemDark() ? 'dark' : 'light') : null) : m;
    if (t) document.documentElement.setAttribute('data-theme', t);
    else document.documentElement.removeAttribute('data-theme');
  }

  var nav = document.querySelector('nav.views');
  var btn, menu;
  if (nav) {
    var box = document.createElement('div');
    box.className = 'kmb-theme';
    box.innerHTML = '<button type="button" class="kmb-theme-btn" aria-haspopup="menu" aria-expanded="false"></button>' +
      '<div class="kmb-theme-menu" role="menu" hidden>' + Object.keys(MODES).map(function (m) {
        return '<button type="button" role="menuitemradio" data-m="' + m + '">' + icon(m) + '<span>' + MODES[m] + '</span></button>';
      }).join('') + '</div>';
    nav.appendChild(box);
    btn = box.firstChild;
    menu = box.lastChild;

    var open = function (on) { menu.hidden = !on; btn.setAttribute('aria-expanded', String(on)); };
    btn.onclick = function () { open(menu.hidden); };
    menu.onclick = function (e) {
      var b = e.target.closest('[data-m]');
      if (!b) return;
      setMode(b.dataset.m);
      open(false);
      btn.focus();
    };
    document.addEventListener('click', function (e) { if (!box.contains(e.target)) open(false); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') open(false); });
  }

  function paint(m) {
    if (!btn) return;
    btn.innerHTML = icon(m);
    btn.title = 'Giao diện: ' + MODES[m];
    btn.setAttribute('aria-label', btn.title);
    menu.querySelectorAll('[data-m]').forEach(function (b) { b.setAttribute('aria-checked', String(b.dataset.m === m)); });
  }

  function setMode(m) {
    if (app) app.setThemeMode(m);
    else try { localStorage.setItem(KEY, m); } catch (e) { }
    apply(m);
    paint(m);
  }

  var mode = getMode();
  apply(mode);
  paint(mode);
})();
