#!/usr/bin/env python3
"""Turn a page exported from Claude (an artifact .html) into the app's offline web bundle.

    python3 tools/prepare_web.py path/to/Ky-Mon-Binh-Phap.html

Writes app/src/main/assets/www/index.html and downloads the Google Fonts the page links to
into app/src/main/assets/www/fonts/, so the app needs no network connection at run time.
The app's own additions (assets/www/app.css and app.js) are kept as they are and linked in.
"""
import html
import os
import re
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WWW = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'www')
FONTS = os.path.join(WWW, 'fonts')
# A current Chrome user agent makes Google Fonts answer with woff2 files.
UA = ('Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) '
      'Chrome/130.0.0.0 Mobile Safari/537.36')

# Artifact pages are fragments: the host adds the document skeleton when it publishes them.
# The inline script sets the light/dark choice before the page is drawn; app.js keeps it after.
HEAD = '''<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<script>(function(){try{var a=window.KmbApp,m=a?a.getThemeMode():localStorage.getItem('kmb-theme');if(a&&m==='auto')m=a.isSystemDark()?'dark':'light';if(m==='light'||m==='dark')document.documentElement.setAttribute('data-theme',m)}catch(e){}})();</script>
'''
APP_CSS = '<link rel="stylesheet" href="app.css">'
APP_JS = '<script src="app.js"></script>'


def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def slug(family):
    return re.sub(r'[^a-z0-9]', '', family.lower())


def bundle_fonts(css_url):
    css = get(css_url).decode('utf-8')
    faces = []
    for m in re.finditer(r'/\*\s*([\w-]+)\s*\*/\s*@font-face\s*\{([^}]*)\}', css):
        subset, body = m.groups()
        prop = lambda k: re.search(k + r'\s*:\s*([^;]+);', body).group(1).strip().strip('\'"')
        url = re.search(r'url\(([^)]+)\)', body).group(1)
        faces.append((url, slug(prop('font-family')), prop('font-style'), prop('font-weight'), subset))
    if not faces:
        sys.exit('No @font-face rules found in ' + css_url)

    # Variable fonts serve several weights from one file: name those "var" instead of a weight.
    weights = {}
    for url, _, _, weight, _ in faces:
        weights.setdefault(url, set()).add(weight)

    os.makedirs(FONTS, exist_ok=True)
    for f in os.listdir(FONTS):
        if f.endswith('.woff2'):
            os.remove(os.path.join(FONTS, f))

    local = {}
    for url, family, style, weight, subset in faces:
        if url in local:
            continue
        w = weight if len(weights[url]) == 1 else 'var'
        name = '%s-%s-%s-%s.woff2' % (family, style, w, subset)
        with open(os.path.join(FONTS, name), 'wb') as out:
            out.write(get(url))
        local[url] = name
    for url, name in local.items():
        css = css.replace(url, name)
    with open(os.path.join(FONTS, 'fonts.css'), 'w', encoding='utf-8') as out:
        out.write(css)

    for family in sorted({f[1] for f in faces}):
        for licence_dir, licence_file in (('ofl', 'OFL.txt'), ('apache', 'LICENSE.txt'), ('ufl', 'UFL.txt')):
            try:
                text = get('https://raw.githubusercontent.com/google/fonts/main/%s/%s/%s'
                           % (licence_dir, family, licence_file))
            except urllib.error.HTTPError:
                continue
            with open(os.path.join(FONTS, 'LICENSE-%s.txt' % family), 'wb') as out:
                out.write(text)
            break
        else:
            print('warning: no licence file found for font family "%s"' % family, file=sys.stderr)
    print('fonts: %d files for %s' % (len(local), ', '.join(sorted({f[1] for f in faces}))))


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    with open(sys.argv[1], encoding='utf-8') as f:
        page = f.read()

    page = re.sub(r'[ \t]*<meta name="claude:[^>]*>\n?', '', page)
    page = re.sub(r'[ \t]*<link rel="preconnect" href="https://fonts\.(googleapis|gstatic)\.com"[^>]*>\n?', '', page)
    m = re.search(r'<link rel="stylesheet" href="(https://fonts\.googleapis\.com/[^"]+)">', page)
    if m:
        bundle_fonts(html.unescape(m.group(1)))
        page = page[:m.start()] + '<link rel="stylesheet" href="fonts/fonts.css">' + page[m.end():]
    # app.css goes after the page's last stylesheet so its rules win; app.js runs after the page's scripts.
    first_script = page.find('<script')
    style_end = page.rfind('</style>', 0, first_script if first_script >= 0 else len(page))
    if style_end >= 0:
        style_end += len('</style>')
        page = page[:style_end] + '\n' + APP_CSS + page[style_end:]
    else:
        page = APP_CSS + '\n' + page
    end = page.lower().rfind('</body>')
    if end < 0:
        end = page.lower().rfind('</html>')
    if end >= 0:
        page = page[:end] + APP_JS + '\n' + page[end:]
    else:
        page = page.rstrip('\n') + '\n' + APP_JS + '\n'
    if not page.lstrip().lower().startswith('<!doctype'):
        page = HEAD + page.lstrip() + '</html>\n'

    os.makedirs(WWW, exist_ok=True)
    with open(os.path.join(WWW, 'index.html'), 'w', encoding='utf-8') as out:
        out.write(page)
    print('wrote', os.path.relpath(os.path.join(WWW, 'index.html'), ROOT))


if __name__ == '__main__':
    main()
