# Nichess

Kotlin ile yazılmış Android satranç uygulaması.

## Özellikler

- Stockfish ile ELO bazlı yapay zeka (800–2800)
- Bluetooth ile iki cihaz arası çevrimiçi oyun
- Aynı cihazda iki kişilik mod
- 4 tahta teması: Klasik, Gece, Orman, Taş
- Gizli mod: siyah ata 5 kez dokun → 10000 ELO rakip + kırmızı tema
- Zaman kontrolü (10 dakika)
- Piyon terfi, rok, geçerken alma

## Kurulum

### Stockfish binary

Stockfish'i [resmi siteden](https://stockfishchess.org/download/) veya
[GitHub Releases](https://github.com/official-stockfish/Stockfish/releases)'dan indir,
aşağıdaki isimlerde `app/src/main/assets/` altına koy:

```
stockfish_arm64    → arm64-v8a cihazlar
stockfish_arm      → armeabi-v7a cihazlar
stockfish_x86_64   → emülatör / x86_64
```

Binary olmadan uygulama yine çalışır, fallback olarak rastgele hamle yapar.

### Build

```bash
./gradlew assembleDebug
```

Release için keystore gerekli:

```bash
export KEYSTORE_FILE=/path/to/release.jks
export KEYSTORE_PASSWORD=xxx
export KEY_ALIAS=xxx
export KEY_PASSWORD=xxx
./gradlew assembleRelease
```

## GitHub Actions

### Secrets (repo settings → Secrets → Actions)

| Secret | İçerik |
|---|---|
| `KEYSTORE_BASE64` | `base64 release.jks` çıktısı |
| `KEYSTORE_PASSWORD` | keystore şifresi |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key şifresi |

Her `main` push'unda debug + release APK artifact olarak yüklenir.
`v*` tag push'unda otomatik GitHub Release oluşturulur.

## Paket

`com.tdev.nichess`

## Bluetooth Oyun

1. Bir cihaz "Oyun Kur" → beyaz olur, bekler
2. Diğer cihaz eşleştirilmiş listeden seçer → siyah olur, bağlanır
3. Cihazların önceden Bluetooth ile eşleştirilmiş olması gerekir
