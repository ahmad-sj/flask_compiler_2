# تقرير مشروع المُصرِّفات (Compilers)

واجهة أمامية لمُصرِّف (Compiler Front-End) للغة مدمجة تجمع **Python + Jinja2 + HTML + CSS**،
مكتوبة بلغة Java اعتماداً على ANTLR 4، مع واجهة خلفية تُصيِّر المدخلات المُحلَّلة إلى موقع ثابت يعمل فعلياً.

> النسخة الإنجليزية من هذا التقرير موجودة في [REPORT.md](REPORT.md).

| الجزء | الموقع |
| --- | --- |
| القواعد النحوية / المحلل اللفظي / المحلل النحوي | [grammars/](grammars/) ← تُولَّد إلى [src/antlr/](src/antlr/) |
| أصناف شجرة النحو المجردة (AST) | [src/models/](src/models/) — ٩٨ صنف عقدة |
| الزوّار (Visitors) | [src/visitors/](src/visitors/) |
| جدول الرموز | [src/symbols/](src/symbols/) |
| طابعة الشجرة | [src/app/TreePrinter.java](src/app/TreePrinter.java) + دالة `print()` في كل عقدة |
| التطبيق التجريبي | [project/](project/) |

---

## ١. القواعد النحوية والمحلل اللفظي والمحلل النحوي

هناك عائلتان من القواعد، فُصلتا لأن اللغتين تختلفان جوهرياً في قواعدهما اللفظية:
فـ Python حسّاسة للإزاحة (Indentation)، بينما القوالب حسّاسة للأنماط (Modes).

| ملف القواعد | الأسطر | ما يغطيه |
| --- | ---: | --- |
| `pythonLexer.g4` | ١٠١ | ٥٢ قاعدة توكن، منها INDENT/DEDENT |
| `pythonParser.g4` | ٢٧٤ | الجُمل، الدوال، المُزخرِفات (Decorators)، التعابير |
| `templateLexer.g4` | ١٥٤ | ١٠٨ قاعدة توكن موزّعة على أنماط لفظية |
| `templateParser.g4` | ٣٣٦ | ٦٨ قاعدة: Jinja و HTML و CSS |
| `templateFragments.g4` | ١١٨ | أجزاء المحارف المشتركة |

**Python.** تُعالَج الإزاحة عبر [`Python3LexerBase`](src/antlr/Python3LexerBase.java)
المُعرَّف كصنف أساسي (`superClass`) للمحلل اللفظي في
[pythonLexer.g4:13](grammars/pythonLexer.g4#L13)، وهو يُولِّد توكنات
INDENT/DEDENT اصطناعية ليستخدمها المحلل النحوي كمُحدِّدات للكتل — وهو الحل المعتاد
للغات ذات «قاعدة الهامش» (Off-side Rule) داخل قواعد خالية من السياق.

**القوالب — الأنماط اللفظية.** ملف القالب هو في الحقيقة أربع لغات متشابكة،
لذلك ينتقل المحلل اللفظي بين أنماط بدل محاولة كتابة مجموعة توكنات واحدة تغطيها كلها:

```
DEFAULT ──'<'──▶ START_TAG_MODE ──'style'──▶ STYLE_START_TAG_MODE ──▶ CSS_BLK
   │                    │                                              │
   │                    └──'="'──▶ ATTR_VAL_QOUTED                      ├──▶ CSS_BLK_PROP
   │                                    │                              │        │
   └──'{{'──▶ EXPRESSION_MODE ◀─────────┘                              │        └──▶ CSS_PROP_VALUES
   └──'{%'──▶ J_STMNT_MODE                                    CSS_INLINE (style="...")
```

ولهذا السبب يُحلَّل `{{ product.name }}` بالطريقة نفسها سواء داخل نص الصفحة
أو داخل `src="…"` — فكلاهما يدفع النمط `EXPRESSION_MODE`.

**تغطية Jinja.** تعابير `{{ }}`، وجُمل `{% %}`، والمرشّحات (Filters) عبر
`concatExpr filter*` حيث `filter : PIPELINE ID (LPAREN argumentList? RPAREN)?`،
وكتل التحكم: `if` / `elif` / `else`، و`for` / `else`، و`set`، و`extends`، و`block`.

**تغطية CSS.** القاعدة `cssBlock : selectorList CSS_LBRACE cssProp* BLK_RBRACE`،
مع مُحدِّدات المعرّف والصنف والعنصر والمُتحدِّر والمجموعة والصنف الزائف (Pseudo-class).

ويظهر ذلك عملياً في كتلة `<style>` داخل
[base.jinja](project/templates/base.jinja) التي تحمل أنماط العناصر التي يملكها
هذا القالب. تشغيلة واحدة على `project/` تبني ٧ عقد `CssBlock` وتُفعِّل خمسة من
أنواع المُحدِّدات الستة: ٩ عنصر، و٢ مُتحدِّر، و١ صنف، و١ مجموعة، و١ صنف زائف.
ويبقى `IdSelector` وحده غير مُستخدَم لعدم وجود سمة `id` في صفحات العرض التجريبي.
أما `style.css` فيُنسخ حرفياً ولا يُحلَّل، ولهذا يمكنه الاحتفاظ بقواعد لا تقبلها
القواعد النحوية مثل `form:not([style*="display: inline"])`.

---

## ٢. شجرة النحو المجردة (AST)

١٠٢ ملف داخل `src/models/`: **٩٨ صنف عقدة** ترث جميعها من الصنف الأساسي المجرّد
الواحد `Node`، إضافةً إلى ثلاث حاويات بسيطة — `App` و`Template` و`RouteInfo` —
*تحتوي* على العقد بدل أن تكون عقداً بذاتها.

كل عقدة تخزّن عناصر الهوية الثلاثة المطلوبة:

```java
public abstract class Node {
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    protected final int nodeId;      // رقم العقدة، فريد لكل تشغيل
    protected String nodeName;       // اسم/نوع العقدة
    protected int lineNumber;        // رقم السطر في المصدر

    protected Node() {
        this.nodeId = ID_SEQUENCE.incrementAndGet();
        this.nodeName = getClass().getSimpleName();   // قيمة افتراضية منطقية
    }
    public String header() { return "#" + nodeId + " " + nodeName + " (line " + lineNumber + ")"; }
    public String print(int level) { ... }            // يُعاد تعريفها في كل صنف فرعي
}
```

تُسنَد الأرقام بترتيب الإنشاء، لذا فهي تسجّل أيضاً الترتيب الذي بنى به المحلل النحوي الشجرة.

### شجرة الوراثة

```
                              Node  (مجرّد)
                                │
      ┌───────────┬─────────────┼──────────────┬──────────────┬────────────┐
      │           │             │              │              │            │
   DocType    NormalText    NodeBody      python.*        jinja.*       html.* / css.*
                                │              │              │            │
                          (قائمة الأبناء)      │              │            │
                                               │              │            ├── HtmlElement (مجرّد)
                    ┌──────────────────────────┤              │            │     ├── HtmlRegularElement
                    │                          │              │            │     ├── HtmlSelfClosingElement
              Func / Decorator          Statement / Value     │            │     └── HtmlStyleElement
              BlockNode                 AssignLine            │            ├── Attribute (مجرّد)
              blocks.IfBlock            ReturnLine            │            │     ├── QuotedAttribute
              blocks.ForNode            ExprLine              │            │     ├── BooleanAttribute
              blocks.WhileNode          import_lines.*        │            │     └── StyleAttribute
              expressions.*             literals.*            │            └── CssBlock / Selector / Property
                                                              │
                          ┌───────────────────────────────────┤
                          │                    │              │
                   JinjaExpression      JinjaBlock (مجرّد)     Expression (مجرّد)
                                              │                     │
                                     IfBlock / ElifBlock       AddExpression / MulExpression
                                     ElseBlock / ForBlock      ComparisonExpression / AndExpression
                                     ExtendsBlock              OrExpression / NotExpression
                                     InheritedBlock            PipeExpression / FilterExpression
                                     SetStatement              PrimaryExpression + trailers.*
                                                               TernaryExpression / atoms.*
```

تعدّد الأشكال (Polymorphism) هو ما يحمل التصميم: فالمُصيِّر والطابعة كلاهما
يتعامل مع مراجع من النوع `Node` ويستدعي `print(level)` أو يوزّع حسب نوع العقدة،
دون معرفة الصنف الفعلي مسبقاً.

### مثال حقيقي على الشجرة

كتلة `{% for %}` في `index.jinja` كما بُنيت فعلياً — أرقام العقد أدناه مقروءة
مباشرة من ملف `compiler_output/ast_jinja.json`:

```
#936 ForBlock (line 10)
 ├── loopVars ──▶ #846 IdType "product"
 ├── iterable ──▶ #848 PrimaryExpression
 │                 └── atom ──▶ #847 IdType "products"
 └── nodeBody ──▶ #935 NodeBody
                   ├── #916 HtmlRegularElement <div class="card">
                   │        └── … img، card-body، العنوان، السعر، رابط التفاصيل
                   └── #934 ElseBlock (line 20)      ← فرع {% else %}
```

يُظهر هذا أمرين. أرقام الأبناء أصغر من رقم الأب، لأن الزائر يبني الأبناء قبل
إنشاء العقدة التي تحتويهم — فالأرقام تسجّل ترتيب الإنشاء. كما أن `{% else %}`
هو **ابن لجسم الحلقة** وليس شقيقاً لها، والمُصيِّر يعتمد على هذا الشكل بالضبط.

أين تظهر الأرقام:

| المُخرَج | أرقام العقد |
| --- | --- |
| `ast_python.json` / `ast_jinja.json` | على **كل** عقدة، تحت المفتاح `"id"` |
| `ast_python.txt` / `ast_jinja.txt` | على سطر ترويسة كل عقدة من المستوى الأعلى |

الأسطر الداخلية في الشجرة النصية تأتي من دالة `print()` الخاصة بكل صنف، وهي
تعرض الاسم والسطر؛ أما ملف JSON فهو العرض الشامل لكل عقدة.

تُبنى الشجرة مرة واحدة و**لا** تُستهلك أثناء التصيير — فالمُصيِّر يمرّ عليها للقراءة
فقط، لذا تبقى سليمة بعد انتهاء التنفيذ وتُكتب إلى `compiler_output/` بعده.

---

## ٣. نمط الزائر (Visitor)

يولّد ANTLR الصنفين `templateParserBaseVisitor<T>` و`pythonParserBaseVisitor<T>`،
ويعيد كل زائر تعريف دوال القواعد التي تهمّه ويُرجع كائنات النموذج.

| الزائر | الدور |
| --- | --- |
| `AppVisitor` | شجرة تحليل Python ← الجذر `App` |
| `PythonVisitor` | قواعد Python ← `models.python.*` (٧٧٠ سطراً) |
| `TemplateVisitor` | جذر القالب ← `Template` |
| `NodeVisitor` | قواعد Jinja و HTML و CSS ← العقد (١٢٧٢ سطراً) |
| `SemanticAnalyzer` | يمرّ على شجرة Python لتنفيذ ١٤ فحصاً |
| `TemplateSemanticAnalyzer` | يفحص كل قالب مقابل السياق الذي يمرّره المسار الذي يُصيّره |
| `PythonDataExtractor` | يمرّ على الشجرة لاستخراج بيانات التصيير |
| `JinjaRenderer` + `ExpressionEvaluator` | يمرّان على الشجرة لإنتاج HTML |

الثلاثة الأخيرة مهمّة: **كل مستهلك هو مرور مستقل على الشجرة نفسها.**
فالتحليل الدلالي والاستخراج والتصيير مراحل منفصلة، ولا يمكن لأيٍّ منها أن يُفسد
الشجرة على البقية.

---

## ٤. جدول الرموز

`SymbolTable` ← `Scope` (شجرة، لكل نطاق أب) ← `Symbol`.

| العملية | الدالة |
| --- | --- |
| الإدراج (insert) | `define(name, kind, type, value)` |
| البحث (lookup) | `resolve(name)` (يرمي استثناءً) / `lookup(name)` (يُرجع null) / `isDefined(name)` |
| التحديث (update) | `update(name, kind, type, value)` — يبحث للخارج بدءاً من النطاق الحالي |
| إدارة النطاقات | `enterScope(name)` / `exitScope()` |
| الفحص | `scopeCount()` / `symbolCount()` / `render()` |

تمشي `Scope.resolve` على سلسلة الآباء، فيرى النطاق الداخلي أسماء الخارجي دون العكس.
وتقوم `Scope.update` بالمرور نفسه، فيؤدي الإسناد إلى اسم مُعرَّف في نطاق محيط إلى
تحديثه هناك بدلاً من حجبه (Shadowing).

المُخرَج من التطبيق التجريبي (١٧ نطاقاً، ١١ رمزاً):

```
symbol         kind          type          value       scope
-------------- ------------- ------------- ----------- ----------------
title          block name    StringType    title       title block
content        block name    StringType    content     content block
product        id            IdType        product     for block at 10:4
```

**مرحلة التوليد لا تستخدم جدول الرموز إطلاقاً.** فالأصناف `CodeGenerator` و
`JinjaRenderer` و `ExpressionEvaluator` و `PythonDataExtractor` لا تحمل أي إشارة
إليه، بل تحلّ الأسماء مقابل السياق المستخرَج، فيبقى التحقّق والإخراج مستقلَّين.

وفي الواقع هناك **جدولان**، والجدول المعروض أعلاه هو جدول القوالب: يملؤه
`NodeVisitor` أثناء *بناء* شجرة AST للقالب، ومن هنا جاءت أسماء `{% block %}`
ومتغيّر حلقة `{% for %}`. أما `SemanticAnalyzer` فيبني جدولاً ثانياً منفصلاً لجهة
Python. إذن الجدول ليس حكراً على التحليل الدلالي، بل هو حكر على الواجهة الأمامية،
والتوليد هو ما يبقى بعيداً عنه.

---

## ٥. طابعة الشجرة

نصفان، كما يصف المطلوب:

1. **دالة طباعة لكل نوع عقدة** — كل صنف يعيد تعريف `print(int level)` فيعرض حقوله
   ويستدعي أبناءه بمستوى `level + 1`. تأتي الإزاحة من `Node.getIndent(level)`.
2. **دالة قيادة تمشي على الشجرة كاملة** — `TreePrinter` يمرّ على الجذور، ويؤطّر كل
   عقدة بـ `node.header()` (الرقم، النوع، السطر)، ثم يستدعي `print(0)`.

```java
TreePrinter.renderPythonAst(app, "app.py");   // شجرة Python
TreePrinter.renderTemplateAsts(templates);    // شجرة كل قالب
TreePrinter.renderSymbolTable(symbolTable);   // جدول الرموز
```

تُطبع على الطرفية أثناء التنفيذ **وأيضاً** تُكتب إلى
`compiler_output/ast_python.txt` و`ast_jinja.txt` و`symbol_table.txt`.
وتُنتَج نسخة JSON قابلة للمعالجة الآلية (`ast_python.json` و`ast_jinja.json`) بجانبها.

نموذج من المُخرَج:

```
[1/10] #8 multi import line (line 2)
----------------------------------------------------------------
multi import
+- line no: 2
+- from name: name
``````+- line no: 2
``````\- id: flask
\- imported names: Flask, render_template, request, redirect, url_for
```

> ملاحظة: ترسم دوال الطباعة الشجرة بمحارف الرسم اليونيكودية `├ └ ─`. إن كانت
> الطرفية تعمل بترميز قديم لا يدعمها، تُحوَّل تلقائياً إلى مقابلاتها ASCII
> (`+ \ -`) كما في المثال أعلاه، حتى لا تظهر على شكل `??`. أما الملفات فتُكتب
> دائماً بترميز UTF-8 بالمحارف الأصلية.

---

## التطبيق التجريبي

[project/app.py](project/app.py) — تطبيق Flask لكتالوج منتجات.
`Product = { id, image, name, price, details }`.

| التدفق | المسار | القالب |
| --- | --- | --- |
| عرض كل المنتجات | `/` | `index.jinja` |
| عرض تفاصيل منتج | `/product/<int:product_id>` | `product_detail.jinja` |
| إضافة منتج | `/add` | `add_product.jinja` |
| تعديل منتج | `/product/<int:product_id>/edit` | `edit_product.jinja` |
| حذف منتج *(إضافي)* | `/product/<int:product_id>/delete` | — |

الشيفرة المصدرية لهذا التطبيق نفسه هي ما يُمرَّر على المحلل اللفظي والنحوي:
ملف `app.py` عبر محلل Python، وملفات `templates/*.jinja` عبر محلل القوالب.

---

## طريقة التشغيل

```powershell
.\build.ps1
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler
```

يطبع شجرة Python، وشجرة كل قالب، وجدول الرموز، ثم يولّد الموقع في `output/`.
ويمكن طباعة كل مرحلة من مراحل الواجهة الأمامية أثناء التنفيذ:

| الخيار | ما يُطبع على الطرفية | الأسطر |
| --- | --- | ---: |
| *(بدون)* | شجرة AST وجدول الرموز | ‏~٩٧٠ |
| `--print-tokens` | وأيضاً تدفّق التوكنات من المحلل اللفظي | ‏~٢٨٠٠ |
| `--print-parse-tree` | وأيضاً أشجار التحليل النحوي من ANTLR | ‏~٦١٠٠ |
| `--print-all` | المراحل الأربع كلها | ‏~٨٠٠٠ |
| `--quiet-ast` | لا شيء | ‏~٦٠ |

وفي كل الأحوال تُكتب كل مرحلة إلى `compiler_output/`: الملفات `tokens.txt`
و`parse_tree.txt` و`ast_python.txt` و`ast_jinja.txt` و`symbol_table.txt`،
إضافة إلى نسخ JSON.

### المراحل الأربع والفرق بينها

```
المصدر ──▶ توكنات المحلل اللفظي ──▶ شجرة التحليل ──▶ AST ──▶ جدول الرموز
           تدفّق مسطّح             عقدة لكل قاعدة    أصناف    الأسماء والأنواع
                                                   النموذج   والنطاقات
```

**التوكنات** هي التدفّق المسطّح، وتُظهر توكنات INDENT/DEDENT الاصطناعية التي
يُولّدها محلل Python، وتبديل الأنماط الذي يقوم به محلل القوالب:

```
98     23:4       INDENT                     '    '   <-- synthetic
 4     3:0        J_STMNT_START              '{%'
```

**شجرة التحليل** هي الشجرة النحوية العيانية (CST): كل قاعدة نحوية طابقت، بما
فيها علامات الترقيم التي تتجاهلها شجرة AST. وهي تُظهر الاشتقاق، مثل نزول
الرمز `'/'` في `@app.route('/')` عبر سلسلة أولويات التعابير كاملة:

```
decorator   (line 25)
  '@'
  name   (line 25)
    id → 'app'
    dotTrailer → '.' 'route'
  callArgs   (line 25)
    callList → callArg → ternaryExpr → orExpr → andExpr
             → equalExpr → compareExpr → addExpr → mulExpr
             → singleExpr → value → baseValue → literal → string
                                                            ''/''
```

**شجرة AST** هي شجرة النموذج — المسار نفسه، بعد إزالة الضجيج (انظر §٥).

افتح `output/index.html` لرؤية الواجهة تعمل. عمليات الإضافة والتعديل والحذف
تُحفظ عبر `localStorage` — انظر [README.md](README.md).

## التحقّق

```powershell
.\check.ps1
```

- ٧ فحوص — بنية المُخرجات، وسلامة الملفات المنسوخة، وصحة JSON، ووجود DOCTYPE، وعدم بقاء وسوم Jinja غير مُصيَّرة
- ٢٧ حالة — التقاط كل واجهة خلفية غير صالحة **ومنع** التوليد عندها
- ٣ حالات — عدم رفض البرامج الصحيحة (حارس ضد الإنذارات الكاذبة، في `tests/valid/`)
- مشروعان — التقاط أخطاء القوالب **ومنع** التوليد عندها (في `tests/bad_templates/`)
- ٥ فحوص — تدفّق تحكّم Jinja (`if` / `elif` / `else`، و`for` / `for-else`)
- ٢٤ فحصاً — الإضافة والتعديل والحذف مُشغَّلة على الصفحات الحقيقية داخل jsdom

مجموعة الإنذارات الكاذبة لا تقلّ أهمية عن البقية: فالمُدقِّق الذي يرفض برنامجاً
صحيحاً أسوأ من الذي يفوته خطأ، لأنه يمنع بناءً كان يجب أن ينجح.
