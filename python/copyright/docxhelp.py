# -*- coding: utf-8 -*-
"""Word 排版的公共部分：字体、页眉、表格都放在这里。"""

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

BLACK = RGBColor(0, 0, 0)


def set_font(run, size, bold=False, cn="宋体", en="Times New Roman"):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = BLACK
    run.font.name = en
    rpr = run._element.get_or_add_rPr()
    fonts = rpr.find(qn("w:rFonts"))
    if fonts is None:
        fonts = OxmlElement("w:rFonts")
        rpr.append(fonts)
    fonts.set(qn("w:ascii"), en)
    fonts.set(qn("w:hAnsi"), en)
    fonts.set(qn("w:eastAsia"), cn)


def refresh_fields(doc):
    """让 Word/WPS 打开时自动更新域，否则页码会一直显示缓存里的“第 1 页”。"""
    settings = doc.settings.element
    if settings.find(qn("w:updateFields")) is not None:
        return
    flag = OxmlElement("w:updateFields")
    flag.set(qn("w:val"), "true")
    compat = settings.find(qn("w:compat"))
    if compat is not None:
        compat.addprevious(flag)
    else:
        settings.append(flag)


def base_doc(body_size=12.0, line=1.5):
    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.top_margin, sec.bottom_margin = Cm(2.54), Cm(2.54)
    sec.left_margin, sec.right_margin = Cm(3.0), Cm(2.6)
    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(body_size)
    normal.font.color.rgb = BLACK
    rpr = normal.element.get_or_add_rPr()
    fonts = rpr.find(qn("w:rFonts"))
    if fonts is None:
        fonts = OxmlElement("w:rFonts")
        rpr.append(fonts)
    fonts.set(qn("w:ascii"), "Times New Roman")
    fonts.set(qn("w:eastAsia"), "宋体")
    if line:
        normal.paragraph_format.line_spacing = line
    normal.paragraph_format.space_before = Pt(0)
    normal.paragraph_format.space_after = Pt(0)
    refresh_fields(doc)
    return doc


def para(doc, text="", size=12.0, bold=False, indent=True, align=None,
         before=0, after=0, line=1.5):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    if line:
        pf.line_spacing = line
    if indent:
        pf.first_line_indent = Pt(size * 2)
    if align is not None:
        p.alignment = align
    if text:
        set_font(p.add_run(text), size, bold)
    return p


def title(doc, text, size=18.0):
    p = para(doc, text, size=size, bold=True, indent=False,
             align=WD_ALIGN_PARAGRAPH.CENTER, before=12, after=18)
    return p


def heading(doc, text, size=14.0):
    return para(doc, text, size=size, bold=True, indent=False, before=12, after=6)


def sub_heading(doc, text, size=12.0):
    return para(doc, text, size=size, bold=True, indent=False, before=8, after=4)


def page_field(p, size=9.0):
    run = p.add_run()
    set_font(run, size)
    for tag, attrs, text in (
        ("w:fldChar", {"w:fldCharType": "begin"}, None),
        ("w:instrText", {"xml:space": "preserve"}, " PAGE "),
        ("w:fldChar", {"w:fldCharType": "separate"}, None),
        ("w:t", {}, "1"),
        ("w:fldChar", {"w:fldCharType": "end"}, None),
    ):
        el = OxmlElement(tag)
        for k, v in attrs.items():
            el.set(qn(k), v)
        if text is not None:
            el.text = text
        run._element.append(el)


def header(section, left_text):
    """页眉：左边软件全称加版本，右边第 X 页。"""
    p = section.header.paragraphs[0]
    for r in list(p.runs):
        r._element.getparent().remove(r._element)
    usable = section.page_width - section.left_margin - section.right_margin
    # 页眉样式里自带居中、右对齐两个制表位，先清掉，否则页码会落在页面中间
    p.style.paragraph_format.tab_stops.clear_all()
    stops = p.paragraph_format.tab_stops
    stops.clear_all()
    stops.add_tab_stop(usable, WD_TAB_ALIGNMENT.RIGHT)
    set_font(p.add_run(left_text), 9)
    p.add_run("\t")
    set_font(p.add_run("第 "), 9)
    page_field(p)
    set_font(p.add_run(" 页"), 9)


def table(doc, rows, widths=None, size=10.5, header_row=False):
    cols = len(rows[0])
    t = doc.add_table(rows=0, cols=cols)
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for idx, row in enumerate(rows):
        cells = t.add_row().cells
        for i, text in enumerate(row):
            if widths:
                cells[i].width = Cm(widths[i])
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            set_font(p.add_run(str(text)), size, bold=(header_row and idx == 0))
    return t


def blank(doc, size=12.0):
    return para(doc, "", size=size, indent=False)


def page_start(section, start):
    """后 30 页接着前 30 页编页码，从 31 起。"""
    sect = section._sectPr
    pg = sect.find(qn("w:pgNumType"))
    if pg is None:
        pg = OxmlElement("w:pgNumType")
        cols = sect.find(qn("w:cols"))
        if cols is not None:
            cols.addprevious(pg)
        else:
            sect.append(pg)
    pg.set(qn("w:start"), str(start))


def image(doc, path, width_cm=13.5, max_height_cm=15.0):
    """插图。手机竖屏截图按宽度算会顶满一页，这里按最大高度回退一下。"""
    from docx.image.image import Image as DocxImage

    try:
        meta = DocxImage.from_file(str(path))
        w, h = meta.px_width, meta.px_height
        if w and h:
            height_cm = width_cm * h / w
            if height_cm > max_height_cm:
                width_cm = width_cm * max_height_cm / height_cm
    except Exception:
        pass
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.first_line_indent = Pt(0)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.add_run().add_picture(str(path), width=Cm(width_cm))
    # 图和图注不能分到两页，否则上一页只有图、下一页只有一行题注
    p.paragraph_format.keep_with_next = True
    return p


def code_doc(cfg, lines, out_path, start_page=1):
    """代码材料：8 磅字、固定行距 13 磅，不加首行缩进。"""
    doc = base_doc(body_size=8, line=None)
    sec = doc.sections[0]
    sec.left_margin, sec.right_margin = Cm(2.2), Cm(2.2)
    header(sec, f"{cfg['software_name']} {cfg['version']}")
    if start_page > 1:
        page_start(sec, start_page)
    for line in lines:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(0)
        pf.space_after = Pt(0)
        pf.line_spacing = Pt(13)
        pf.widow_control = False
        set_font(p.add_run(line), 8, cn="宋体", en="Consolas")
    doc.save(str(out_path))
