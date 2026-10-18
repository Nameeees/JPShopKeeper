# Publicacion en SpigotMC - JPShopKeeper 1.1.0

Guia paso a paso para publicar el recurso. Lo que ya esta hecho esta marcado;
lo que depende de ti, tambien.

---

## 1. Lo que ya esta listo

- [x] Compila limpio (`mvn clean package`) con bytecode **Java 8** contra Spigot API 1.8.8.
- [x] JAR verificado: `plugin.yml` con la version inyectada, 4 archivos de config,
      sin dependencias empaquetadas ni ofuscacion.
- [x] Textos en **espanol e ingles** (`settings.language`) con las mismas claves
      y los mismos slots en ambos idiomas (comparado automaticamente).
- [x] **PlaceholderAPI** opcional y **Vault** opcional: sin ellos el plugin arranca igual.
- [x] Sin llamadas a internet, sin telemetria, sin descargas externas.
- [x] Licencia **MIT** (`LICENSE`) y **CHANGELOG.md**.
- [x] Pagina del recurso escrita en BBCode: `docs/spigotmc-descripcion-es.bbcode`
      y `docs/spigotmc-descripcion-en.bbcode`, con las imagenes ya colocadas.
- [x] Icono (512x512) y 6 infografias (1280x720) **en espanol e ingles**, en
      `docs/assets/`, generadas con `docs/assets/generate_assets.py`.

### Archivo que se sube

```
target/JPShopKeeper-1.1.0.jar
```

Ese es el unico archivo que va al recurso. El codigo fuente (`src/`) no se sube.

## 2. Antes de darle a publicar: prueba en un servidor limpio

- [ ] **Servidor 1.8.8 limpio** (Paper/Spigot) solo con el JAR:
      `/trader create tienda` -> editar una pagina -> crear un tradeo ->
      comprar como jugador -> reiniciar el servidor -> el NPC sigue ahi.
- [ ] **Servidor de tu version mas moderna, limpio**: lo mismo.
- [ ] **Sin Vault**: un tradeo con precio debe avisar y no cobrar.
- [ ] **Sin PlaceholderAPI**: la consola no debe mostrar ningun error al arrancar.
- [ ] **Con Vault + PlaceholderAPI**: `/papi parse me %jpsk_traders%` devuelve un numero.
- [ ] **/trader reload** y apagado del servidor sin errores en consola.

## 3. Crear el recurso

En SpigotMC: **Resources -> Add Resource**.

| Campo | Valor sugerido |
| --- | --- |
| Resource name | `JPShopKeeper` |
| Tag line | `Trade NPCs with pages, one JAR for 1.8 - latest. No Citizens, no NMS. Vault optional.` |
| Category | La mas cercana del desplegable (Gameplay / QoL / Mechanics) |
| Native Minecraft Version | `1.8` (es la mas antigua soportada) |
| Tested Minecraft Versions | Marca desde **1.8** hasta la ultima que hayas probado |
| Version | `1.1.0` |
| Price | Gratuito (el recurso esta bajo licencia MIT) |
| Description | Pega `docs/spigotmc-descripcion-es.bbcode` o `...-en.bbcode` |

**Truco para la descripcion**: en el editor del recurso activa el modo
**BB code** (boton del editor) y pega el contenido tal cual. Si pegas en el
editor visual, los `[LIST]` y los colores se pueden perder.

**Idioma de la descripcion**: las normas de SpigotMC exigen que la descripcion
este en **ingles** (regla 9 del foro). Lo normal es publicar
`docs/spigotmc-descripcion-en.bbcode`; si quieres ofrecerla tambien en
espanol, pega la inglesa y anade al final
`[SPOILER="Descripcion en espanol"]` con la otra.

## 4. Imagenes (todas generadas en `docs/`)

Sube cada PNG al recurso (quedan como adjuntos) y sustituye su marcador en la
descripcion por la URL que devuelva SpigotMC:

| Archivo | Tamano | Marcador | Donde va |
| --- | --- | --- | --- |
| `docs/banner.png` | 1280x720 | `URL_DEL_BANNER` / `URL_OF_BANNER` | Arriba del todo, bajo el titulo. |
| `docs/assets/<idioma>/features.png` | 1280x720 | `URL_FEATURES` | Tras la introduccion. |
| `docs/assets/<idioma>/versions.png` | 1280x720 | `URL_VERSIONS` | Tras la lista de caracteristicas. |
| `docs/assets/<idioma>/modern.png` | 1280x720 | `URL_MODERN` | Extras modernos (hex, MiniMessage, cabezas, PlaceholderAPI). |
| `docs/assets/<idioma>/setup.png` | 1280x720 | `URL_SETUP` | En la instalacion. |
| `docs/assets/<idioma>/commands.png` | 1280x720 | `URL_COMMANDS` | En comandos y permisos. |
| `docs/assets/<idioma>/trades.png` | 1280x720 | `URL_TRADES` | En configuracion. |
| `docs/assets/<idioma>/npcs.png` | 1280x720 | `URL_NPCS` | En las preguntas frecuentes. |
| `docs/assets/icon.png` | 512x512 | — | **En el formulario** (icono del recurso), no en la descripcion. |
| `docs/assets/discord.png` | 1280x260 | `URL_DISCORD` | Al final, dentro del bloque de comunidad. |
| `docs/assets/documentation.png` | 1280x260 | `URL_DOCS` | Al final, dentro del bloque de comunidad. |

El juego de infografias existe en **espanol** (`docs/assets/es/`) y en
**ingles** (`docs/assets/en/`): usa el que corresponda al idioma de la
descripcion que publiques. Todas se regeneran (o se retocan sus textos) con:

```
python docs/assets/generate_assets.py
```

## 5. Capturas reales (recomendado)

Las infografias explican; las capturas lo demuestran. Con el plugin ya probado
en un servidor, sube 3 o 4 capturas:

1. El menu de tradeos abierto en un NPC, con varias ofertas a la vista.
2. El editor de un tradeo (coste, resultado, precio y permiso).
3. La consola al arrancar, con la linea "NPC generado para el trader ...".
4. Opcional: el mismo NPC en un servidor 1.8, para demostrar la compatibilidad.

## 6. Reglas de SpigotMC que te afectan

Resumen de las *SpigotMC Terms & Rules* (leelas completas antes de publicar):

- **Nada de enlaces con pago por clic** (adf.ly y similares) y los enlaces de
  descarga externos deben ser directos. Aqui no aplica: el JAR se sube al
  propio recurso.
- **Ofuscacion**: permitida, pero los recursos gratuitos solo pueden usar
  ofuscacion basica de nombres. Este JAR **no esta ofuscado**, que es lo mas
  seguro para que lo aprueben.
- **Software de terceros**: si el recurso necesita otro plugin hay que decirlo
  en la descripcion. Aqui Vault y PlaceholderAPI son **opcionales** y asi se
  indica.
- **No usar marcas de otros servidores** para promocionar el recurso.
- **Publicidad**: permitida en la pagina del recurso con limites (secundaria
  al recurso y fuera de la primera mitad de la descripcion). Si no anuncias
  nada, mejor.
- **No publiques plugins de otros**: el codigo debe ser tuyo.

Los recursos nuevos pasan por **moderacion** antes de ser visibles: es normal
esperar unas horas.

## 7. Despues de aprobar

- [ ] Anade la URL del recurso al `README.md` y, si quieres, a `plugin.yml`:
      `website: https://www.spigotmc.org/resources/xxxxx/` (y recompila).
- [ ] Vigila la pestana **Discussion** los primeros dias: responde rapido y
      con enlace al changelog.
- [ ] Para reportes de errores, pide siempre: version del servidor, version del
      plugin, pasos para reproducirlo y el error completo de la consola.
- [ ] Cuando corrijas algo, publica una **update** con su entrada en
      `CHANGELOG.md` y sube el nuevo JAR (mismo nombre con la version nueva).

## 8. Plantilla para responder a un reporte de error

```
Gracias por el reporte. Para poder reproducirlo necesito:

1. Version del servidor (por ejemplo Paper 1.21.11) y version del plugin.
2. Si tienes Vault y/o PlaceholderAPI instalados.
3. Pasos exactos: que comando ejecutaste y que hiciste en el menu.
4. El error completo de la consola (desde la linea "Could not pass event"
   o la excepcion completa, no solo la primera linea).
5. Si afecta a un trader concreto, su archivo en plugins/JPShopKeeper/traders/.
```
