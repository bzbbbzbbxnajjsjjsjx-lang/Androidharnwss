# Vardiya

Material 3 (Jetpack Compose) vardiya ve kazanç takip uygulaması.

- **Özet**: Tek büyük dalgalı (wavy) yuvarlak ilerleme; vardiya süresine doğru dolar, ortasında sayaç ve canlı kazanç. Altta Başlat / Bitir tuşları; vardiya sürerken Mola ver / Moladan dön (molada sayaç ve ücret durur).
- **Takvim**: Aylık takvim; vardiyalı günler, resmi tatiller ve arifeler (13:00'ten itibaren) işaretli. Güne dokunarak vardiya ekle. *Çalışma saatleri* sekmesi: hangi gün kaç saat çalışıldığı. *Raporlar* sekmesi: haftalık ve aylık raporlar.
- **Vardiyalar**: Ayın vardiya listesi, vardiya başı kazanç; her vardiya onayla silinebilir.
- **Ayarlar**: Aylık maaş (varsayılan 28.000 ₺) ya da doğrudan saatlik ücret, aylık saat (225), fazla mesai (×1,5) ve tatil (×2) çarpanları.

## Hesaplama

- Saatlik ücret = aylık maaş ÷ 225 saat (28.000 ₺ → 124,44 ₺/saat), ya da ayarlarda girilen saatlik ücret.
- Resmi tatil / bayram saatleri ×2 (İş Kanunu md. 47), arife ve 28 Ekim 13:00'ten itibaren.
- Ayda 225 saati aşan çalışma fazla mesai, ×1,5 (md. 41).
- Mola süresi ücretten düşülür. Gece yarısını geçen vardiyalar ertesi güne doğru sayılır.

## Derleme

```bash
./gradlew assembleDebug
```

Her push'ta GitHub Actions APK'yı derler; **Actions → Android → vardiya-apk** altından indirilebilir.
