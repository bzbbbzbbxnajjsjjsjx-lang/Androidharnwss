## Vardiya

Material 3 vardiya ve kazanç takip uygulaması.

**Kurulum:** Aşağıdaki `Vardiya-*.apk` dosyasını telefona indir ve aç (bilinmeyen kaynaklardan yüklemeye izin vermen gerekebilir). Android 8.0 ve üzeri.

### Yenilikler (1.7.0)
- **Mola:** Vardiya sürerken Başlat/Bitir'in üstünde **Mola ver** tuşu var. Moladayken sayaç ve kazanç durur, "Molada · ücret durdu" yazar; **Moladan dön** ile devam edilir. Toplam mola ekranda görünür ve vardiyaya kaydedilir, ücrete dahil edilmez.
- **Takvim'e yeni sekmeler:** *Çalışma saatleri* sekmesinde ayın her çalışılan günü, kaç saat çalışıldığı, saat aralıkları, mola ve kazanç; üstte ayın toplamı. *Raporlar* sekmesinde haftalık (Pazartesi–Pazar) ve aylık raporlar: çalışılan süre, vardiya ve gün sayısı, günlük ortalama, mola, fazla mesai, bayram saatleri ve kazanç.
- **Vardiya silme:** Vardiyalar listesinde ve takvimdeki gün penceresinde her vardiyanın yanında silme tuşu; silmeden önce onay sorulur.
- **Saatlik ücret:** Ayarlar'da "Saatlik ücretimi kendim gireceğim" açılınca aylık maaş yerine saatlik ücret doğrudan girilebilir.
- **Dalgalı halka:** F-Droid'in büyük dalgalı göstergesinin oranları (daha kalın çizgi, daha uzun dalga).

### 1.6.2
- **Vardiya başında halka boş görünmüyor:** Vardiya başlar başlamaz halkada akan bir dalga görünüyor (8 saatlik vardiyanın ilk ~25 dakikasında halka bu en küçük boyda kalır, sonra gerçek orana göre dolar). Önceden ilk dakikalarda sadece bir nokta görünüyordu.

### 1.6.1
- **Halka vardiyanın süresiyle doluyor:** Artık her dakika başa dönmüyor; geçen süre ile vardiya süresinin (Ayarlar'daki saat) oranında, akıcı şekilde doluyor. 8 saatlik vardiyada 4. saatte halka yarı dolu; süre aşılınca dolu kalır.
- **Dalgalı halka artık Material'ın kendi bileşeni:** Elle çizilen halka yerine Material 3 Expressive'in resmi dalgalı ilerleme göstergesi; dalgalar düzgün ve eşit, vardiya yokken halka düz ve boş görünüyor.

### 1.6.0
- **Material 3 Expressive tam sürüm:** Uygulama Material 3'ün 1.5 sürümüne geçti; Expressive bileşenler artık deneysel değil.
- **Yeni alt menü:** Daha ince (64dp) Expressive menü, seçili sekmenin simgesi hap biçimli bir arka planla vurgulanıyor.
- **Başlat ve Bitir butonları:** Resmi Expressive butonlar; basınca köşeleri yaylanarak kareleşiyor.
- **Vurgulu yazılar:** Süre ve aylık kazanç Material'ın vurgulu yazı stilleriyle gösteriliyor; animasyonlar Expressive yay hareketlerini kullanıyor.

### 1.5.2
- **Dalgalı halka resmi Material 3 Expressive oranlarına çekildi:** Dalga boyu çizgi kalınlığının 3,75 katı (halkada 21 dalga), düz kısım dalgalı kısımla aynı kalınlıkta, dalga saniyede bir dalga boyu akar.

### 1.5.1
- **Performans:** Vardiya sürerken Takvim ve Vardiyalar ekranları artık her saniye yeniden çizilmiyor; saniyelik güncelleme sadece Özet ekranında çalışıyor.
- **Erişilebilirlik (TalkBack):** Takvim günleri tarih, tatil, vardiya kazancı ve "bugün" bilgisiyle okunuyor; halka ortası ve aylık kazanç tek öğe olarak okunuyor; başlıklar başlık olarak işaretli.

### 1.5
- **Haptik geri bildirim:** Başlat'ta onay titreşimi, Bitir'de daha güçlü titreşim, alt barda sekme değişince hafif tık, vardiya sürerken her dakika sonunda (halka bir tur dönünce) hafif tık.
- **Dalgalı halka Material 3 Expressive'e yaklaştırıldı:** Dalga vardiya başlayınca yumuşakça büyür, bitince düzleşir; ilerleme ile arka plan arasında boşluk, kalın yuvarlak uçlar.

### 1.4
- **Dalgalı halka yeniden yazıldı:** Material'ın alfa bileşeni yerine kendi çizdiğimiz dalgalı halka; dalga sürekli akar ve halka her dakika bir tur dolar, ilerleme gözle görülür. Vardiyanın yüzdesi halkanın içinde yazar.

### 1.3
- **Sadeleşen Özet ekranı:** Tek büyük dalgalı yuvarlak ilerleme. Vardiya başlayınca vardiya süresine (8 saat) doğru dolar; ortasında sayaç ve o vardiyanın canlı kazancı, altında ayın toplam kazancı. En altta **Başlat** ve **Bitir** tuşları.

### 1.2
- **Para canlı artıyor:** Vardiya sürerken "Bu ay kazancın" toplamı, maaş ilerleme çubuğu ve çalışılan süre her saniye güncellenir.
- **Düzen:** Duvar kâğıdından gelen gri renkler yerine uygulamanın kendi renkleri; Özet'teki üst üste binen "+ Vardiya" tuşu kaldırıldı (Vardiyalar sekmesinde duruyor); dalgalı çubuklar düşük yüzdelerde de dalgalı; yüzdeler ondalıklı.

### 1.1
- **Vardiyayı Başlat / Bitir tuşu:** Başlatınca süre canlı sayılır, dalgalı çubuk vardiya süresine (varsayılan 8 saat) doğru dolar, o ana kadarki kazanç gösterilir. Bitir'e basınca vardiya kaydedilir. Uygulama kapansa da sayaç devam eder.
- **Girişte şekil animasyonu:** Açılışta şekil değiştiren M3 Expressive yükleme göstergesi; ilerleme göstergeleri sıfırdan dolarak açılır, yüzde rozeti daireden kurabiye şekline dönüşür. Tuşlar basılınca köşe şekli değişir.

### Özellikler
- **Özet:** Ayın kazancı, maaş hedefine dalgalı (wavy) ilerleme çubuğu, çalışılan saat için dairesel dalgalı gösterge; normal / fazla mesai / bayram dökümü.
- **Takvim:** Vardiyalı günler, resmi tatiller ve arifeler işaretli; güne dokunarak vardiya ekleme.
- **Vardiyalar:** Ayın vardiya listesi ve vardiya başı kazanç.
- **Ayarlar:** Aylık maaş (varsayılan 28.000 ₺), aylık saat (225), fazla mesai (×1,5) ve tatil (×2) çarpanları, vardiya süresi (8 saat).

### Hesaplama
- Saatlik ücret = maaş ÷ 225 saat (28.000 ₺ → 124,44 ₺/saat)
- Bayram ve resmi tatil saatleri ×2 (arife ve 28 Ekim 13:00'ten itibaren)
- Ayda 225 saati aşan çalışma ×1,5
- Ücretsiz mola süresi düşülür

> APK debug anahtarıyla imzalıdır; Play Store'a yüklemek için kendi imza anahtarınla imzalanması gerekir.
