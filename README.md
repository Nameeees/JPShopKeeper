# JPShopKeeper

**Version 1.1.0** · [Documentacion (EN)](https://nameeees.github.io/JPShopKeeper/) · [Codigo en GitHub](https://github.com/Nameeees/JPShopKeeper)

NPCs **comerciantes** para Minecraft con tradeos y paginas configurables
desde un menu de cofre. Un solo JAR funciona **desde la 1.8 hasta la ultima
version** del juego (Spigot/Paper), sin NMS, sin Citizens y con Vault
opcional para los tradeos con dinero.

```
Clic derecho en el NPC   ->  menu de tradeos (paginas de ofertas)
Shift + clic derecho     ->  panel de administracion (crear/editar)
```

---

## Caracteristicas

- **NPCs de mob nativo** (aldeano por defecto, tipo configurable): invulnerables,
  sin IA, silenciosos en todas las versiones (en 1.8 por NBT), **anclados a su
  punto e inamovibles: ni empujandolos con la hitbox ni con knockback** (tarea
  de anclaje por tick; en 1.8 tambien, donde no existe `setCollidable`), con
  mirada automatica hacia el jugador mas cercano y la GUI de comercio vanilla
  del aldeano siempre cerrada. Si el NPC desaparece, se regenera solo.
- **Tradeos flexibles**: hasta 3 items de coste + coste en monedas (Vault),
  resultado, permiso requerido y enfriamiento por jugador. Comparacion de
  items configurable (nombre, lore, encantamientos, durabilidad, NBT).
- **Soporte NBT total (items de otros plugins)**: tokens, vouchers o items
  con datos personalizados se guardan tal cual (NBT crudo) y, si se usan
  como coste, solo se acepta un item del jugador con ese mismo NBT.
- **Paginas**: cada trader tiene tantas paginas de tradeos como quieras
  (hasta 45, una por slot del grid).
- **Editor 100% en menus**: paginas, tradeos, nombre del NPC, tipo de mob,
  movimiento, previsualizacion como jugador y confirmaciones para todo lo
  destructivo. Nada de editar archivos a mano (aunque tambien se puede).
- **Compatibilidad total 1.8 - ultima version**: materiales, sonidos y
  entidades se resuelven por nombre en tiempo de ejecucion; los metodos
  que cambiaron entre versiones se invocan por reflexion con fallbacks.
- **Textos y menus 100% configurables** en `messages.yml` y `menus.yml`
  (formato legacy `&`, el unico soportado por la 1.8).
- **Espanol e ingles incluidos**: `settings.language` en `config.yml` elige el
  idioma. Anadir otro es copiar los textos a `messages_XX.yml` y
  `menus_XX.yml`: no hay que tocar codigo.
- **PlaceholderAPI opcional**: placeholders `%jpsk_...%` para scoreboards,
  hologramas y tablists (numero de traders, paginas, tradeos, enfriamiento
  del jugador...).
- **Todo lo moderno, sin romper la 1.8**: colores hexadecimales `&#RRGGBB`
  (1.16+, aproximados en versiones antiguas), `custom-model-data` en los iconos
  (1.14+), cabezas de jugador en `menus.yml`, titulos largos, mobs de versiones
  nuevas en `npc.allowed-types` y **MiniMessage opcional** en Paper 1.16+
  (`settings.mini-message`).
- **Cooldowns persistentes**: los enfriamientos activos sobreviven a los
  reinicios (`cooldowns.yml`).
- **18 pruebas automaticas** (`mvn test`) sobre la logica pura: materiales
  entre versiones, validacion de numeros y cantidades.

---

## Instalacion

1. Descarga o compila `JPShopKeeper-1.1.0.jar`.
2. Colocalo en la carpeta `plugins/` de tu servidor (1.8+).
3. Opcional: instala **Vault** + un plugin de economia para tradeos con
   dinero y **PlaceholderAPI** si quieres usar los placeholders.
4. Reinicia el servidor. Se crean `config.yml`, `messages.yml` y `menus.yml`.
5. Para los textos en ingles, pon `settings.language: en` en `config.yml`:
   se crean `messages_en.yml` y `menus_en.yml`.

### Compilar desde el codigo

```
mvn clean package
```

Requiere JDK 8+ y Maven. El JAR se genera en `target/`.
El proyecto se compila **contra Spigot API 1.8.8 con bytecode Java 8**
a proposito: es lo que permite que el mismo archivo cargue en la 1.8 y
en la ultima version.

### Verificacion en vivo

Probado con servidores reales: **Paper 1.8.8** (arranque, carga de
traders, generacion del NPC con noAI por NMS, comandos y recarga) y
**Paper 1.21.11** (arranque, carga/guardado de tradeos con items,
generacion y adopcion del NPC, `/trader list`, `/trader reload` y
apagado limpio), ambos sin errores en consola.

Ademas, `mvn test` ejecuta 18 pruebas automaticas de la logica pura
(materiales por version, validacion de numeros y cantidades de los tradeos).

---

## Estructura del proyecto

```
src/main/java/com/jp/shopkeeper/
├── JPShopKeeper.java      Nucleo: cablea managers, listeners y comando.
├── command/               TraderCommand: /trader (crear, editar, listar...).
├── listener/              Eventos: interaccion con NPCs, proteccion, menus y chat.
├── menu/                  Los 6 menus de cofre (tradeos, editor, paginas, confirmacion).
├── manager/               Logica: traders/NPCs, tradeos, economia (Vault), textos, menus.yml.
├── model/                 Trader, TradePage y Trade (los datos).
├── storage/               Persistencia YAML (un archivo por trader).
├── hook/                  PlaceholderAPI (se carga solo si el plugin existe).
└── util/                  Compatibilidad multi-version + helpers (texto, items, sonidos).

src/test/java/             Pruebas de la logica pura (mvn test).
docs/                      Pagina del recurso y guia de publicacion en SpigotMC.
```

---

## Comandos y permisos

| Comando | Permiso | Descripcion |
| --- | --- | --- |
| `/trader help` | — | Ayuda con los comandos disponibles. |
| `/trader create <id> [mob]` | `jpshopkeeper.admin.create` | Crea un trader donde estas. |
| `/trader edit <id>` | `jpshopkeeper.admin.edit` | Abre el panel de edicion. |
| `/trader rename <id> <nombre>` | `jpshopkeeper.admin.edit` | Renombra el NPC. |
| `/trader list` | `jpshopkeeper.admin.edit` | Lista todos los traders. |
| `/trader tp <id>` | `jpshopkeeper.admin.teleport` | Te teletransporta al trader. |
| `/trader remove <id>` | `jpshopkeeper.admin.remove` | Elimina un trader y sus datos. |
| `/trader reload` | `jpshopkeeper.admin.reload` | Recarga configs y traders. |

Alias del comando: `/traders`, `/jpsk`.

| Permiso | Por defecto | Descripcion |
| --- | --- | --- |
| `jpshopkeeper.use` | todos | Abrir los menus de tradeo de los NPCs. |
| `jpshopkeeper.admin` | operadores | Acceso completo (hereda todos los hijos). |
| `jpshopkeeper.admin.create` | operadores | Crear traders. |
| `jpshopkeeper.admin.edit` | operadores | Editar traders (panel shift + clic derecho, edit, rename, list). |
| `jpshopkeeper.admin.remove` | ops | Eliminar traders. |
| `jpshopkeeper.admin.teleport` | ops | Teletransportarse a un trader. |
| `jpshopkeeper.admin.reload` | ops | Recargar la configuracion. |

Cada tradeo puede exigir ademas un permiso propio (campo "Permiso" en el editor).

---

## Flujo de administracion

1. Escribe `/trader create mi_tienda`. Nace un aldeano-NPC y se abre el panel.
2. En el panel: **Renombrar NPC**, **Tipo de mob**, **Paginas y tradeos**,
   **Ver como jugador**, **Traer NPC aqui** y **Eliminar trader** (con confirmacion).
3. En **Paginas y tradeos** pulsa **Anadir pagina** y entra en ella:
   el marco y los 28 slots del grid son exactamente lo que vera el jugador
   (lo que coloques en el editor aparece igual en el menu del jugador).
4. Clic en un slot vacio crea un tradeo. Dentro del editor:
   - Clic en un slot de **coste** o del **resultado** (se ilumina) y despues
     clic en un item de tu inventario: queda asignado (se copia, no se consume).
   - **Clic derecho** en un slot con item: ajustar cantidad por chat.
   - **Shift + clic derecho**: vaciar el slot.
   - **Dinero / Permiso / Enfriamiento**: clic y escribir el valor por chat.
   - **Guardar tradeo** valida que haya resultado y vuelve al grid.
   - **Shift + clic** sobre un tradeo del grid: eliminarlo (con confirmacion).
5. Los jugadores abren el menu con **clic derecho** en el NPC y compran con
   un clic. El icono muestra en vivo si te falta dinero, items o estas en
   enfriamiento.

---

## Configuracion

### `config.yml`

- `npc`: mob por defecto y lista de mobs permitidos, mirada automatica,
  intervalo de mantenimiento, anclaje anti-empuje (`anchor-interval-ticks`),
  distancia de anclaje, proteccion y persistencia.
- `sounds`: que suena en cada evento (listas de nombres candidatos por version).
- `trades.match`: como se comparan los items del coste (nombre, lore,
  encantamientos, durabilidad, NBT), comportamiento con inventario lleno y
  maximo de paginas.

### `messages.yml`

Todos los textos visibles. Placeholders entre llaves (`{trader}`, `{money}`...)
y colores con `&`. El prefijo vive en la clave `prefix`.

### `menus.yml`

Apariencia de los 6 menus (titulos, botones, relleno, plantillas de lore).
Los slots ya vienen validados; puedes cambiar materiales, nombres, lore y
mover botones editando su `slot`.

Cada icono admite ademas, en versiones que lo soportan:

| Clave | Version | Efecto |
| --- | --- | --- |
| `custom-model-data: 1234` | 1.14+ | Textura del resource pack. |
| `head: NombreJugador` | todas | Cabeza de ese jugador (en linea o en la cache). |

Colores: `&` legacy y, desde la 1.16, tambien `&#RRGGBB`.

### Idiomas

`settings.language` (en `config.yml`) decide que archivo de textos se carga:

| Valor | Archivos | Notas |
| --- | --- | --- |
| `es` | `messages.yml` + `menus.yml` | Por defecto. |
| `en` | `messages_en.yml` + `menus_en.yml` | Se extraen del JAR la primera vez. |
| otro | `messages_XX.yml` + `menus_XX.yml` | Copia los textos y traduce. |

Si el archivo del idioma no existe, se usa el archivo base: un idioma sin
traduccion nunca deja el plugin sin textos.

### Formato de los textos

- `&` legacy (`&a`, `&l`...), compatible con todas las versiones.
- `&#RRGGBB` en 1.16+ (en versiones anteriores se aproxima al color legacy
  mas parecido, asi que el mismo archivo vale en cualquier servidor).
- MiniMessage (Paper 1.16+) activando `settings.mini-message: true`: permite
  `<red>`, `<bold>`, `<gradient:#00ff00:#0088ff>`... Se puede mezclar con
  los codigos `&` en el mismo texto.

### Placeholders (PlaceholderAPI)

Con PlaceholderAPI instalado y `hooks.placeholderapi: true`, el plugin
registra estos placeholders:

| Placeholder | Devuelve |
| --- | --- |
| `%jpsk_traders%` | Numero de traders. |
| `%jpsk_maxpages%` | Maximo de paginas por trader. |
| `%jpsk_exists_<id>%` | si / no. |
| `%jpsk_name_<id>%` | Nombre visible del NPC. |
| `%jpsk_world_<id>%` | Mundo del NPC. |
| `%jpsk_x_<id>%` · `%jpsk_y_<id>%` · `%jpsk_z_<id>%` | Coordenadas del NPC. |
| `%jpsk_pages_<id>%` | Paginas del trader. |
| `%jpsk_trades_<id>%` | Tradeos totales del trader. |
| `%jpsk_cooldown_<id>_<pagina>_<slot>%` | Segundos que le quedan al jugador (0 = listo). |
| `%jpsk_ready_<id>_<pagina>_<slot>%` | si / no. |

El numero de pagina empieza en 1 (igual que en el menu) y el slot es el numero
visible del editor (1-28). Los textos "si" y "no" se cambian en
`messages.yml` (claves `placeholder.yes` y `placeholder.no`).

### Datos: `traders/<id>.yml`

Un archivo por trader, editable a mano (usa `/trader reload` al terminar):

```yaml
display-name: "&aComerciante"
entity-type: VILLAGER       # cualquier mob de npc.allowed-types
profession: ""              # opcional (solo aldeanos): FARMER, LIBRARIAN...
entity-uuid: 00000000-0000-0000-0000-000000000000
look-at-players: true
location:
  world: world
  x: 100.5
  y: 64.0
  z: -20.5
  yaw: 180.0
pages:
  '0':
    name: "Basicos"
    trades:
      '10':                 # slot visible del grid (10-16, 19-25, 28-34, 37-43)
        cost:
          '0':
            item: <item>            # mapa estandar legible (Bukkit)
            nbt: "<base64>"         # solo si el item tiene NBT de otros plugins
        result:
          item: <item>
          nbt: "<base64>"
        money: 500.0        # coste en monedas (Vault), opcional
        permission: ""      # permiso requerido, opcional
        cooldown: 30        # segundos por jugador, opcional
```

---

## Compatibilidad (como funciona)

| Pieza | 1.8 | 1.9 - 1.12 | 1.13+ | Ultimas versiones |
| --- | --- | --- | --- | --- |
| Materiales | nombres modernos traducidos a `STAINED_GLASS_PANE:7` etc. | nombres modernos | nombres modernos | nombres modernos |
| Sonidos | `CLICK`, `LEVEL_UP`... | `UI_BUTTON_CLICK`... | idem | idem (enum, campo o clave de recurso) |
| Sin IA | etiqueta `NoAI` en el NBT del mob | `setAI(false)` | idem | idem |
| Silencio | etiqueta `Silent` en el NBT del mob | `setSilent(true)` | idem | idem |
| Colisiones/gravedad/persistencia | se omiten si no existen | segun version | idem | `setPersistent`, `setCollidable`... |
| GUI de comercio vanilla | se cancela y se abre el menu del plugin | idem | idem | idem |

Reglas internas del proyecto (importantes si vas a tocar el codigo):

- **Nunca** referenciar constantes de `Material`, `Sound`, `EntityType` o
  `Enchantment` directamente: se resuelven por nombre (`Materials`, `Sounds`,
  `Entities`, `ItemBuilder`) porque esos enums cambiaron entre versiones.
- Todo lo que no existia en 1.8 se invoca con `Reflect.call(...)` o `Nms`
  (reflexion con fallback): si el metodo no existe, simplemente se omite.
- Cero NMS directo: solo `getHandle()` por reflexion, mas las etiquetas
  `NoAI` y `Silent` del NBT del mob para la 1.8 (ese NMS no tiene `setNoAI`
  ni `setSilent`; comprobado con javap sobre `patched_1.8.8.jar`).
- Textos solo en `messages.yml`/`menus.yml`; numeros ajustables en `config.yml`.

---

## Preguntas frecuentes

**¿Puedo usar skins de jugador en los NPCs?**
No: para que un solo JAR funcione de la 1.8 a la ultima version sin romperse,
los NPCs son mobs nativos. Un NPC con skin de jugador exigiria paquetes
distintos por version (o Citizens).

**¿Los NPCs sobreviven a reinicios?**
Si. Se guardan su UUID y posicion; al arrancar el plugin los "adopta" y, si
alguno no existe (por un crash o un /kill), lo regenera automaticamente.

**¿Que pasa si no tengo Vault?**
Los tradeos de items funcionan igual. Los que cuestan dinero muestran
"el servidor no tiene economia" y no se cobran.

**¿Puedo clonar un trader?**
Copia su archivo `.yml` en `traders/`, cambia el nombre del archivo (el id)
y usa `/trader reload`. Ajusta su `location` para no superponerlo.

**¿Como cambio el idioma?**
Pon `settings.language: en` en `config.yml` y usa `/trader reload` (o
reinicia). Se crean entonces `messages_en.yml` y `menus_en.yml` en la
carpeta del plugin, listos para editar.

**¿Que pasa si desinstalo el plugin?**
Los NPCs ya creados se quedan en el mundo (son mobs persistentes e
invulnerables). Antes de quitar el plugin, elimina los traders con
`/trader remove <id>`.

---

## Solucion de problemas

**La consola repite "NPC generado/regenerado para el trader X" cada pocos
segundos y en el juego no aparece nada.**

Otro plugin o una proteccion del servidor esta **cancelando la aparicion de
mobs** en ese punto (`CreatureSpawnEvent`). Es lo tipico de:

- Regiones de **WorldGuard** con `mob-spawning: deny`.
- Claims de **GriefPrevention** con bloqueo de mobs.
- Limitadores/limpiadores de mobs que cancelan apariciones.

Con la version corregida veras un aviso claro en consola con las coordenadas
exactas y un solo aviso cada 30 segundos (no spam). Soluciones:

- Pon el flag de la region en permitir: `/rg flag <region> mob-spawning allow`.
- O mueve el trader a una zona sin proteccion:
  `/trader edit <id>` → **Traer NPC aqui**.
- Tras arreglar la proteccion, el NPC se genera solo (se reintenta cada 30s).

**El NPC aparece pero desaparece al rato.**

Algo lo esta eliminando despues de crearse (limpiadores de entidades,
stackers de mobs, comandos de limpieza). La consola te avisara con el
trader afectado. Excluye a los NPCs de comerciante de ese plugin, o sube
`npc.protect` (ya viene activado por defecto y cancela todo el dano).

---

## Documentacion para la publicacion

- `docs/spigotmc-descripcion-es.bbcode` y `docs/spigotmc-descripcion-en.bbcode`:
  la pagina del recurso lista para pegar en SpigotMC (en modo BB code).
- `docs/spigotmc-checklist.md`: campos del formulario, reglas aplicables y
  pruebas recomendadas antes de publicar.
- `docs/banner.png`: imagen de cabecera del recurso (1280x720).
- `docs/assets/`: icono (512x512) y 6 infografias (1280x720) en espanol e
  ingles, generadas con `docs/assets/generate_assets.py` (Pillow).
- `docs/compatibilidad-versiones.md`: que funciones modernas cubre el plugin,
  cuales no y cuanto costaria anadirlas (MiniMessage, Folia, Brigadier...).

---

## Licencia

[MIT](LICENSE): puedes usar y modificar el plugin citando la autoria.

