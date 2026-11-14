# Market (Sunucu Mağazası)

`/market` → SUNUCU MAĞAZASI menüsü (END, NETHER, ALETLER, YEMEK, KRİSTALLER).
Menüde bir ürünün üstüne **farenin ORTA TUŞUYLA** tıklayınca envanterindeki aynı türdeki tüm eşyalar satılır.

Komutlar: `/market` (`/magaza`), `/bakiye`, `/marketadmin reload`,
`/marketadmin <para|kristal> <ver|al|ayarla> <oyuncu> <miktar>`

Ayarlar: `plugins/Market/config.yml` (ürün, fiyat, kategori, mesaj). Değişince `/marketadmin reload`.

## GitHub ile derleme
1. github.com → **New repository** → adı `market` → **Create repository**.
2. Bu zip'i bilgisayarında aç. İçindeki **her şeyi** (pom.xml, src klasörü, .github klasörü, README.md) repoya yükle: **Add file → Upload files** → sürükle-bırak → **Commit changes**.
   - `.github` klasörü yüklenmezse: **Add file → Create new file** → ad kısmına `.github/workflows/build.yml` yaz → dosyanın içeriğini yapıştır → Commit.
3. Üstteki **Actions** sekmesine gir. "Build" çalışıyor olmalı (sarı daire). Yeşil tik olunca tıkla.
4. Sayfanın altındaki **Artifacts** bölümünden **Market-jar**'ı indir, zip'ten `Market.jar` çıkar.
5. `Market.jar` dosyasını sunucunun `plugins` klasörüne at, sunucuyu yeniden başlat.
6. Oyunda `/market` yaz.
