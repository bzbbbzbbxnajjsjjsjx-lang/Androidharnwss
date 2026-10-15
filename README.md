# Vardiya

Material 3 (Jetpack Compose) vardiya ve kazanç takip uygulaması.

- **Özet**: Ayın kazancı, maaş hedefine dalgalı (wavy) ilerleme çubuğu, çalışılan saat için dairesel dalgalı gösterge, normal / fazla mesai / bayram dökümü.
- **Takvim**: Aylık takvim; vardiyalı günler, resmi tatiller ve arifeler (13:00'ten itibaren) işaretli. Güne dokunarak vardiya ekle.
- **Vardiyalar**: Ayın vardiya listesi, vardiya başı kazanç.
- **Ayarlar**: Aylık maaş (varsayılan 28.000 ₺), aylık saat (225), fazla mesai (×1,5) ve tatil (×2) çarpanları.

## Hesaplama

- Saatlik ücret = aylık maaş ÷ 225 saat (28.000 ₺ → 124,44 ₺/saat).
- Resmi tatil / bayram saatleri ×2 (İş Kanunu md. 47), arife ve 28 Ekim 13:00'ten itibaren.
- Ayda 225 saati aşan çalışma fazla mesai, ×1,5 (md. 41).
- Mola süresi ücretten düşülür. Gece yarısını geçen vardiyalar ertesi güne doğru sayılır.

## Derleme

```bash
./gradlew assembleDebug
```

Her push'ta GitHub Actions APK'yı derler; **Actions → Android → vardiya-apk** altından indirilebilir.
