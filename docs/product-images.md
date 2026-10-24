# Demo product images

Migration V6 assigns four ordered images to each of the 60 demo products using
the five AI-generated sets supplied in `brillen-produktbilder/`. The 20 PNG files are copied
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
shapes and types. The landing page also uses the front-view tortoiseshell AI image.
V6 replaces the earlier image assignments without changing V1–V5,
so both existing and fresh databases receive the new sets.

## Historical migrations

V4 and V5 contain paths and dimensions for six earlier Mister Spex sample photos.
Those files and the earlier landing-page photo have been removed from the current
tree. Git history is unchanged. Keep the applied migrations unchanged to preserve
Flyway checksums: V6 replaces all earlier assignments with the AI images, including
when initializing a fresh database. No old photographs are needed at startup.

Every image has positive source dimensions returned by the API; Next.js handles
responsive resizing. `main_thumbnail` selects the catalog image, while
`rotating_thumbnail` independently selects rotation images. A nonempty gallery
must have exactly one main thumbnail at transaction commit.
