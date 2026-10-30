from pathlib import Path
import argparse
import html
import re

BASE=Path(__file__).resolve().parent
SOURCES=BASE.parent
OUTPUT=BASE.parents[2]/'build/privacy-preview'
CSS=(BASE/'privacy-preview-style.css').read_text()
SLUGS=['verantwortlich','geltungsbereich','daten-und-zwecke','rechtsgrundlagen','empfaenger','speicherdauer','rechte','minderjaehrige','aenderungen']
LOCALES={
 'de': {
  'path':'/', 'appendix':'## Quellen und Prüfhinweise',
  'title':'Datenschutzerklärung', 'description':'Prüffassung der Datenschutzerklärung für die ST-Testumgebung der 한마음교회-App.',
  'skip':'Zum Inhalt springen', 'brand_top':'한마음교회 – zum Seitenanfang', 'language':'Sprache', 'contact':'Datenschutzkontakt',
  'eyebrow':'TRANSPARENZ & VERANTWORTUNG', 'intro':'Wie wir personenbezogene Daten in unserer ST-Testversion und den damit verbundenen Diensten verarbeiten.',
  'language_meta':'ST-Testumgebung · Deutsche Prüffassung', 'date':'Bearbeitungsstand 28. Oktober 2026', 'status':'Bearbeitungsstand',
  'draft_label':'ENTWURF ZUR ABSTIMMUNG', 'draft_body':'Diese Fassung enthält noch offene Angaben und ist nicht zur Veröffentlichung freigegeben.',
  'review_link':'Prüfhinweise ansehen', 'overview':'Auf einen Blick', 'controller':'VERANTWORTLICHE GEMEINDE',
  'location':'Frankfurt am Main, Deutschland', 'contact_label':'DATENSCHUTZKONTAKT', 'contact_sub':'Kirchenbüro · 이충만 목사', 'phone_label':'Kirchenbüro',
  'scope_label':'GELTUNGSBEREICH', 'scope':'ST-App & angebundene Dienste', 'scope_sub':'Hinweise zur Verarbeitung Ihrer Daten',
  'toc_label':'AUF DIESER SEITE', 'toc_aria':'Inhaltsverzeichnis',
  'toc':['Verantwortliche Stelle','Geltungsbereich','Daten & Zwecke','Rechtsgrundlagen','Empfänger & Standorte','Speicherdauer','Ihre Rechte','Altersgruppe & Entscheidungen','Änderungen'],
  'good':'Gut zu wissen', 'good_text':'Diese Datenschutzerklärung informiert Sie. Ihre Kenntnisnahme ist keine Einwilligung.',
  'note':'Noch zu klären', 'table':'Datenschutzübersicht', 'available':'WIR SIND FÜR SIE DA', 'questions':'Fragen zu Ihren Daten?',
  'contact_text':'Bei Datenschutzanfragen, einem Widerruf oder einer Löschanfrage hilft die Kontaktstelle im Kirchenbüro. App-bezogene Anfragen können Sie zusätzlich per E-Mail stellen.',
  'editorial':'Redaktionelle Prüfhinweise & Quellen', 'text_date':'Textgrundlage: Arbeitsentwurf, aktualisiert am 28. Oktober 2026.',
  'editorial_text':'Die deutschen, koreanischen und englischen Arbeitsfassungen sind vollständig parallel verfügbar. Offene Angaben bleiben bei den jeweiligen Abschnitten als aufklappbare Prüfpunkte sichtbar. Die Fassungen sind nicht zur Veröffentlichung freigegeben.',
  'top':'Zurück nach oben',
 },
 'en': {
  'path':'/en/', 'appendix':'## Sources and review notes',
  'title':'Privacy notice', 'description':'Working draft of the privacy notice for the ST test environment of the 한마음교회 app.',
  'skip':'Skip to content', 'brand_top':'한마음교회 – back to top', 'language':'Language', 'contact':'Privacy contact',
  'eyebrow':'TRANSPARENCY & RESPONSIBILITY', 'intro':'How we process personal data in our ST test version and its connected services.',
  'language_meta':'ST test environment · English draft', 'date':'Last edited 28 October 2026', 'status':'Document status',
  'draft_label':'DRAFT FOR REVIEW', 'draft_body':'This version still contains open items and has not been approved for publication.',
  'review_link':'View review notes', 'overview':'At a glance', 'controller':'RESPONSIBLE CHURCH',
  'location':'Frankfurt am Main, Germany', 'contact_label':'PRIVACY CONTACT', 'contact_sub':'Church office · Pastor 이충만', 'phone_label':'Church office',
  'scope_label':'SCOPE', 'scope':'ST app & connected services', 'scope_sub':'Information about the processing of your data',
  'toc_label':'ON THIS PAGE', 'toc_aria':'Table of contents',
  'toc':['Responsible organisation','Scope','Data & purposes','Legal bases','Recipients & locations','Retention','Your rights','Age group & decisions','Changes'],
  'good':'Good to know', 'good_text':'This privacy notice provides information. Acknowledging it does not give consent.',
  'note':'To be clarified', 'table':'Privacy overview', 'available':'GET IN TOUCH', 'questions':'Questions about your data?',
  'contact_text':'For privacy enquiries, withdrawal of consent or a deletion request, contact our church office by phone. You can also send app-related enquiries by email.',
  'editorial':'Editorial review notes & sources', 'text_date':'Text baseline: working draft, updated on 28 October 2026.',
  'editorial_text':'The German, Korean and English working drafts are available in full alongside one another. Open items remain visible as expandable review notes in the relevant sections. These versions are not approved for publication.',
  'top':'Back to top',
 },
 'ko': {
  'path':'/ko/', 'appendix':'## 출처 및 검토 참고사항',
  'title':'개인정보 처리방침', 'description':'한마음교회 앱 ST 테스트 환경의 개인정보 처리방침 검토용 초안입니다.',
  'skip':'본문으로 바로가기', 'brand_top':'한마음교회 – 맨 위로', 'language':'언어', 'contact':'개인정보 문의',
  'eyebrow':'투명하게 안내하고 책임 있게 관리합니다', 'intro':'ST 테스트 버전과 연결된 서비스에서 개인정보를 어떤 목적으로, 어떻게 처리하는지 안내합니다.',
  'language_meta':'ST 테스트 환경 · 한국어 초안', 'date':'최종 수정일 2026년 10월 28일', 'status':'문서 검토 상태',
  'draft_label':'검토를 위한 초안', 'draft_body':'아직 확인이 필요한 사항이 포함되어 있으며 공개 승인 전 문서입니다.',
  'review_link':'검토 참고사항 보기', 'overview':'주요 정보', 'controller':'책임 있는 교회',
  'location':'독일 프랑크푸르트암마인', 'contact_label':'개인정보 관련 문의', 'contact_sub':'교회 사무실 · 이충만 목사', 'phone_label':'교회 사무실',
  'scope_label':'적용 범위', 'scope':'ST 앱 및 연결된 서비스', 'scope_sub':'개인정보 처리에 관한 안내',
  'toc_label':'이 페이지의 내용', 'toc_aria':'목차',
  'toc':['개인정보 처리 책임자','적용 범위','처리 정보와 목적','법적 근거','수신자와 처리 지역','보관 기간','이용자의 권리','이용 연령과 자동화된 결정','처리방침 변경'],
  'good':'알아두세요', 'good_text':'이 처리방침은 정보를 안내하는 문서입니다. 내용을 확인하는 것만으로 동의가 이루어지지는 않습니다.',
  'note':'확인이 필요한 사항', 'table':'개인정보 처리 현황', 'available':'문의해 주세요', 'questions':'개인정보에 관해 궁금하신가요?',
  'contact_text':'개인정보 관련 문의, 동의 철회 또는 삭제 요청은 교회 사무실로 연락해 주세요. 앱 관련 문의는 이메일로도 보내실 수 있습니다.',
  'editorial':'편집용 검토 참고사항 및 출처', 'text_date':'본문 기준: 2026년 10월 28일에 수정한 검토용 초안입니다.',
  'editorial_text':'독일어, 한국어 및 영어 초안 전체를 함께 제공합니다. 아직 확인이 필요한 사항은 해당 절의 펼쳐볼 수 있는 검토 항목에 그대로 표시합니다. 세 언어 모두 공개 승인 전 문서입니다.',
  'top':'맨 위로',
 },
}

def esc(value):return html.escape(value)

def inline(value):
 value=esc(value).replace('&lt;br&gt;','<br>')
 value=re.sub(r'\[([^\]]+)\]\((https://[^\s)]+)\)',r'<a href="\2" target="_blank" rel="noopener noreferrer">\1</a>',value)
 value=re.sub(r'\*\*([^*]+)\*\*',r'<strong>\1</strong>',value)
 return re.sub(r'`([^`]+)`',r'<code>\1</code>',value)

def blocks(source,ui):
 result=[]
 for raw in re.split(r'\n\s*\n',source.strip()):
  raw=raw.strip()
  if not raw or raw=='---':continue
  if re.match(r'^\*\*\[(OFFEN|OPEN|확인 필요)',raw):
   value=raw.removeprefix('**[').removesuffix(']**')
   value=re.sub(r'^(OFFEN|OPEN|확인 필요)\s*:?\s*','',value)
   result.append('<details class="review-note"><summary><span class="note-mark" aria-hidden="true">!</span>'+esc(ui['note'])+'<span class="plus" aria-hidden="true">+</span></summary><div>'+inline(' '.join(value.splitlines()))+'</div></details>')
  elif raw.startswith('|'):
   rows=[]
   for i,line in enumerate(raw.splitlines()):
    if re.fullmatch(r'[\s|:\-]+',line):continue
    tag='th' if i==0 else 'td'
    rows.append('<tr>'+''.join(f'<{tag}'+(' scope="col"' if tag=='th' else '')+'>'+inline(c.strip())+f'</{tag}>' for c in line.strip().strip('|').split('|'))+'</tr>')
   result.append('<div class="table-scroll" role="region" aria-label="'+esc(ui['table'])+'" tabindex="0"><table><thead>'+rows[0]+'</thead><tbody>'+''.join(rows[1:])+'</tbody></table></div>')
  elif raw.startswith('- '):
   result.append('<ul>'+''.join('<li>'+inline(line[2:])+'</li>' for line in raw.splitlines() if line.startswith('- '))+'</ul>')
  elif raw.startswith('### '):
   lines=raw.splitlines();result.append('<h3>'+inline(lines[0][4:])+'</h3>')
   if len(lines)>1:result.append('<p>'+inline(' '.join(lines[1:]))+'</p>')
  else:result.append('<p>'+inline(' '.join(raw.splitlines()))+'</p>')
 return '\n'.join(result)

def build(locale,ui):
 text=(SOURCES/f'privacy-policy.{locale}.draft.md').read_text()
 body,appendix=text.split(ui['appendix'],1)
 body=body[body.index('## 1. '):].strip().removesuffix('---').strip()
 replacements={
  'de':[('Eine eigene Kurseinwilligung aus #290','Eine eigene Kurseinwilligung'),('ist Teil von #290 und noch nicht ausgeliefert.','ist vorgesehen und noch nicht ausgeliefert.')],
  'en':[('The separate course consent from #290','The separate course consent'),('is part of #290 and has','is planned and has')],
  'ko':[('이슈 #290의 별도 교육 과정 동의','별도 교육 과정 동의'),('기록하는 기능은 이슈 #290에서','기록하는 기능은')],
 }
 for old,new in replacements[locale]:body=body.replace(old,new)
 sections=[]
 parts=re.split(r'^## (\d+)\. (.+)\n',body,flags=re.M)
 for i in range(1,len(parts),3):
  n=int(parts[i]);heading=parts[i+1];content=parts[i+2]
  sections.append(f'<section id="{SLUGS[n-1]}" class="policy-section" aria-labelledby="heading-{n}"><div class="section-heading"><span class="section-number" aria-hidden="true">{n:02}</span><h2 id="heading-{n}">{inline(heading)}</h2></div>'+blocks(content,ui)+'</section>')
 if len(sections)!=9:raise ValueError(f'{locale}: incomplete sections')
 nav=''.join(f'<li><a href="#{slug}"><span aria-hidden="true">{i:02}</span>{esc(label)}</a></li>' for i,(slug,label) in enumerate(zip(SLUGS,ui['toc']),1))
 names={'de':'Deutsch','ko':'한국어','en':'English'}
 language_nav='<nav class="language-nav" aria-label="'+esc(ui['language'])+'">'+''.join('<a href="'+LOCALES[lang]['path']+'" lang="'+lang+'" hreflang="'+lang+'"'+(' aria-current="page"' if lang==locale else '')+'>'+names[lang]+'</a>' for lang in ['de','ko','en'])+'</nav>'
 alternate=''.join('<link rel="alternate" hreflang="'+lang+'" href="'+value['path']+'">' for lang,value in LOCALES.items())
 page=f'''<!doctype html>
<html lang="{locale}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="color-scheme" content="light dark"><meta name="robots" content="noindex, nofollow"><meta name="description" content="{esc(ui['description'])}"><title>{esc(ui['title'])} · 한마음교회</title>{alternate}<style>{CSS}</style></head>
<body id="seitenanfang"><a class="skip" href="#erklaerung">{esc(ui['skip'])}</a>
<div class="container"><header class="site-header"><a class="brand" href="#seitenanfang" aria-label="{esc(ui['brand_top'])}"><svg class="brand-symbol" viewBox="0 0 90 75" fill="#b6cb33" aria-hidden="true" focusable="false"><rect x="20" y="0" width="17" height="17" rx="2.7"/><rect x="39" y="6" width="12" height="11" rx="1.8"/><rect x="0" y="19" width="17" height="17" rx="2.7"/><rect x="20" y="19" width="17" height="17" rx="2.7"/><rect x="39" y="19" width="17" height="17" rx="2.7"/><rect x="59" y="19" width="8" height="9" rx="1.3"/><rect x="77" y="22" width="6" height="6" rx="1"/><rect x="67" y="29" width="7" height="7" rx="1.1"/><rect x="86" y="29" width="4" height="4" rx="1"/><rect x="20" y="39" width="17" height="17" rx="2.7"/><rect x="39" y="39" width="9" height="8" rx="1.3"/><rect x="20" y="58" width="17" height="17" rx="2.7"/></svg><span><span class="brand-name" lang="ko">한마음교회</span><span class="brand-sub">HANMAUM CHURCH</span></span></a><div class="header-actions">{language_nav}<a class="header-link" href="#kontakt-heading">{esc(ui['contact'])} <span aria-hidden="true">↗</span></a></div></header>
<main id="erklaerung"><div class="hero"><p class="eyebrow">{esc(ui['eyebrow'])}</p><h1>{esc(ui['title'])}</h1><p class="intro">{esc(ui['intro'])}</p><div class="meta"><span>Android & iOS</span><span>{esc(ui['language_meta'])}</span><span>{esc(ui['date'])}</span></div></div>
<aside class="draft-banner" aria-label="{esc(ui['status'])}"><span class="draft-label">{esc(ui['draft_label'])}</span><p>{esc(ui['draft_body'])}</p><a href="#redaktion">{esc(ui['review_link'])} ↓</a></aside>
<div class="at-a-glance" aria-label="{esc(ui['overview'])}"><div class="fact"><div class="fact-label">{esc(ui['controller'])}</div><strong lang="de">Koreanische Kirchengemeinde Hanmaum</strong><small>{esc(ui['location'])}</small></div><div class="fact"><div class="fact-label">{esc(ui['contact_label'])}</div><strong><a href="tel:+496933999677">+49 69 3399 9677</a></strong><small>{esc(ui['contact_sub'])}</small></div><div class="fact"><div class="fact-label">{esc(ui['scope_label'])}</div><strong>{esc(ui['scope'])}</strong><small>{esc(ui['scope_sub'])}</small></div></div>
<div class="layout"><aside class="sidebar"><nav class="toc" aria-label="{esc(ui['toc_aria'])}"><p class="toc-label">{esc(ui['toc_label'])}</p><ol>{nav}</ol></nav><p class="sidebar-note"><strong>{esc(ui['good'])}</strong><br>{esc(ui['good_text'])}</p></aside><article class="policy" aria-label="{esc(ui['title'])}">{''.join(sections)}</article><aside class="contact-card" aria-labelledby="kontakt-heading"><div class="contact-overline">{esc(ui['available'])}</div><h2 id="kontakt-heading">{esc(ui['questions'])}</h2><p>{esc(ui['contact_text'])}</p><div class="contact-links"><a href="tel:+496933999677">{esc(ui['phone_label'])}: +49 69 3399 9677 <span aria-hidden="true">↗</span></a><a href="mailto:hanmaum.dev@gmail.com">hanmaum.dev@gmail.com <span aria-hidden="true">↗</span></a></div></aside></div>
<details id="redaktion" class="editorial"><summary>{esc(ui['editorial'])}</summary><div class="editorial-content"><p><strong>{esc(ui['text_date'])}</strong> {esc(ui['editorial_text'])}</p>{blocks(appendix.split(chr(10),1)[1],ui)}</div></details></main>
<footer class="site-footer"><div><span class="footer-brand" lang="ko">한마음교회</span> · Leonhard-Heißwolf-Str. 5 · 65936 Frankfurt am Main</div><div><a href="#seitenanfang">{esc(ui['top'])} ↑</a></div></footer></div></body></html>'''
 output=OUTPUT/('index.html' if locale=='de' else f'{locale}/index.html')
 output.parent.mkdir(parents=True,exist_ok=True);output.write_text(page)
 print(locale,output,'sections=',len(sections),'notes=',page.count('class="review-note"'))

if __name__=='__main__':
 parser=argparse.ArgumentParser(description='Build the unchanged DE/KO/EN privacy drafts for review.')
 parser.add_argument('--output',type=Path,default=OUTPUT)
 args=parser.parse_args()
 OUTPUT=args.output.resolve()
 for locale,ui in LOCALES.items():build(locale,ui)
