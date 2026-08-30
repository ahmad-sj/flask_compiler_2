# ملاحظة: متى تحتاج فعلياً لإعادة توليد المشروع؟

## الادعاء الشائع (غير الدقيق)

> "أي تعديل على الواجهة (مثل إضافة منتج) يستلزم إعادة التوليد (render) ليتزامن الخرج مع البيانات"

## الحقيقة الفعلية بالمشروع

هذا الادعاء **معكوس تماماً**. المشروع مصمَّم خصيصاً بحيث لا تحتاج تعديلات المستخدم من المتصفح (إضافة/تعديل/حذف منتج) إلى إعادة تشغيل المترجم إطلاقاً.

## الدليل من الكود

### 1. `project/script.js`

تعليق صريح في أعلى الملف:

```js
/*
 * Client runtime for the generated static site.
 *
 * The generated pages are static files, so add / edit / delete have no server
 * to post to. This keeps the collection in localStorage instead - seeded once
 * from the build-time data in data.js, and treated as the source of truth from
 * then on.
 */
```

### 2. آلية العمل الفعلية

1. **وقت البناء (Build Time)**: `CodeGenerator.writeClientData()` في
   [src/app/CodeGenerator.java](src/app/CodeGenerator.java#L149) يكتب ملف `output/data.js` الذي يحمل:
   - `window.__SITE_DATA__` — بيانات الموديول وقت البناء (مثل قائمة `products`)
   - `window.__SITE_ROUTES__` — بيانات وصفية عن كل مسار (route)، تكفي المتصفح لإعادة بناء روابط لعناصر لم تكن موجودة وقت البناء

2. **أول زيارة بالمتصفح**: `script.js` يقرأ `data.js` وينسخ محتواه إلى `localStorage`.

3. **من هذه اللحظة فصاعداً**: `localStorage` يصبح "مصدر الحقيقة" (Source of Truth)، وليس `data.js` بعد الآن. أي إضافة/تعديل/حذف من الواجهة يُكتب مباشرة إلى `localStorage` عبر الدوال `readStore()`/`writeStore()` في `script.js`.

### 3. لماذا هذا التصميم ضروري أصلاً؟

الموقع الناتج (`output/`) هو ملفات HTML/CSS/JS **ثابتة بلا أي خادم (backend)**. لا توجد قاعدة بيانات ولا واجهة برمجية (API) يمكن للمتصفح أن يرسل لها طلب حفظ. الحل الوحيد المتاح عملياً هو الاعتماد على تخزين المتصفح المحلي (`localStorage`).

## الخلاصة: متى تحتاج فعلياً لإعادة التوليد؟

**تحتاج إعادة تشغيل المترجم فقط عند تعديل المصدر نفسه:**
- `app.py` (بيانات ابتدائية جديدة، مسارات (routes) جديدة، منطق مختلف)
- ملفات `.jinja` (تغيير بنية الصفحات نفسها)

**لا تحتاج إعادة توليد عند:**
- إضافة/تعديل/حذف عنصر من الواجهة نفسها في المتصفح (يُحفظ تلقائياً في `localStorage` بلا أي اتصال بالمترجم أو بخادم)

## المراجع بالكود

- [project/script.js](project/script.js) — التعليق التوضيحي في الأعلى، ودوال `readStore()`/`writeStore()`
- [src/app/CodeGenerator.java](src/app/CodeGenerator.java#L149) — الدالة `writeClientData()`
