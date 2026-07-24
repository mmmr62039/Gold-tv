# Insecure GeckoView Browser

یک پروژه ساده اندروید با **GeckoView** که لینک‌های `http` و `https` را با موتور داخلی Gecko باز می‌کند و تمام‌صفحه اجرا می‌شود.

## ویژگی‌ها

- استفاده از Mozilla GeckoView، نه WebView سیستم اندروید
- پشتیبانی از لینک‌های ناامن `http://` با `usesCleartextTraffic=true`
- تمام‌صفحه و مخفی‌کردن status/navigation bar
- قابل بیلد شدن در GitHub Actions
- دریافت لینک از Intent مرورگر (`http` و `https`)


## خروجی ABI جداگانه

GitHub Actions برای هر معماری یک APK جدا می‌سازد و هر APK را داخل ZIP جداگانه قرار می‌دهد:

- `armeabi-v7a` برای ARM 32-bit
- `arm64-v8a` برای ARM 64-bit، رایج‌ترین حالت گوشی‌های جدید
- `x86` برای شبیه‌سازها یا دستگاه‌های قدیمی Intel 32-bit
- `x86_64` برای شبیه‌سازها یا دستگاه‌های Intel 64-bit

در هر آپدیت لازم نیست همه APKها را نصب کنید؛ فقط ZIP مربوط به معماری دستگاه خودتان را از artifact دانلود و APK داخلش را نصب کنید.

## بیلد در GitHub

1. فایل‌ها را داخل یک ریپازیتوری GitHub آپلود کنید.
2. به تب **Actions** بروید.
3. workflow با نام **Build Android APK** را اجرا کنید.
4. بعد از پایان، APK از بخش **Artifacts** قابل دانلود است.

## تغییر صفحه پیش‌فرض

در فایل زیر مقدار `HOME_URL` را تغییر دهید:

```java
app/src/main/java/com/example/insecurebrowser/MainActivity.java
```

> هشدار: باز کردن لینک‌های ناامن HTTP برای اطلاعات حساس امن نیست.
