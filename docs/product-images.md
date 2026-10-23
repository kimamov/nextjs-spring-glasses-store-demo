# Demo product images

Migration V6 assigns four ordered images to each of the 60 demo products using
the five sets supplied in `brillen-produktbilder/`. The 20 PNG files are copied
unchanged to `frontend/public/images/products/` and served at `/images/products/`.
Sets are reused by catalog color:

| Catalog color | Image filename group | Dimensions |
| --- | --- | --- |
| Black, Navy | `brille-01-schwarz-bild-01` through `04` | 384 × 204 |
| Tortoiseshell | `brille-02-havanna-bild-01` through `04` | 384 × 205 |
| Champagne | `brille-03-gold-bild-01` through `04` | 384 × 205 |
| Clear | `brille-04-transparent-grau-bild-01` through `04` | 384 × 205 |
| Olive | `brille-05-olivgruen-bild-01` through `04` | 384 × 205 |

Image 01 is the main thumbnail. All four images are selected for future rotation.
The images illustrate fictional products; their frames may differ from catalog
shapes and types. The supplied folder does not include provenance or licensing
information. V6 replaces the earlier image assignments without changing V1–V5,
so both existing and fresh databases receive the new sets.

## Earlier samples

Downloaded from Mister Spex on 23 October 2026 for this learning project:

| Demo product | Photograph source | Local files |
| --- | --- | --- |
| Mitte | [Ray-Ban RX 2447V 5494](https://www.misterspex.de/p/pg/6536001) | `frontend/public/images/products/misterspex-6536001-*` |
| Riviera | [Ray-Ban Round RB 2447 1157](https://www.misterspex.de/p/sg/6534459) | `frontend/public/images/products/misterspex-6534459-*` |

Each sample has the angled (`a`), side (`b`), and front (`d`) photographs from
the source gallery. Original image URLs follow this pattern:

```text
https://media.misterspex.com/dpr_1/f_auto,w_500,c_pad/productimages/6536001/6536001_a
https://media.misterspex.com/dpr_1/f_auto,w_500,c_pad/productimages/6534459/6534459_a
```

The files retain their original JPEG or PNG format and are served locally.
All six local source files measure 500 × 332 pixels. Migration V5 backfills these
measured dimensions; every image requires positive `width` and `height` values,
returned by the API in pixels. These describe the source file, not its displayed
size. Next.js handles responsive resizing. If additional images were inserted
manually before V5, extend its backfill with their measured dimensions before
applying it; unknown dimensions are not filled with invented defaults.
These are illustrative photos: the catalog names, brands, shapes, and prices
remain fictional and do not describe the source products. Replace the samples
with owned or licensed product photographs before public or commercial use.

V4 originally seeds three ordered images for each sample product. `main_thumbnail`
selects the image shown on catalog cards; `rotating_thumbnail` independently
marks images for a future rotation feature. To change the main selection, update
the old and new flags in one transaction. A product may have no images, but a
nonempty image list must have exactly one main selection at commit. The other
58 demo products originally retained the placeholder until V6.
