# Changelog

Todos los cambios publicados de JPShopKeeper.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el versionado [SemVer](https://semver.org/lang/es/).

## [1.1.0] - 2026-10-18

Primera version publica (la 1.0.0 nunca llego a publicarse: todo lo que se
corrigio y se anadio en la revision previa va incluido aqui).

### Anadido

- **NPCs comerciantes de mob nativo** (sin NMS y sin Citizens): invulnerables,
  sin IA, silenciosos donde la version lo permite, anclados a su punto
  (inmunes a empujones y knockback), con mirada automatica al jugador mas
  cercano y regeneracion automatica si el NPC desaparece.
- **Tradeos flexibles**: hasta 3 items de coste, coste en monedas (Vault
  opcional), resultado, permiso requerido y enfriamiento por jugador.
- **Soporte NBT completo**: los items de otros plugins (tokens, vouchers,
  objetos con datos propios) se guardan en NBT crudo y, usados como coste,
  solo se aceptan con ese mismo NBT.
- **Comparacion de items configurable**: nombre, lore, encantamientos,
  durabilidad y NBT.
- **Paginas ilimitadas** (hasta 45 por trader) con 28 tradeos por pagina.
- **Editor 100% en menus**: renombrar NPC, tipo de mob, paginas, tradeos,
  previsualizacion como jugador, mover el NPC y confirmaciones para las
  acciones destructivas.
- **Compatibilidad 1.8 - ultima version con un solo JAR**: materiales,
  sonidos y entidades resueltos por nombre; los metodos que cambiaron entre
  versiones se invocan por reflexion con respaldo.
- **Textos y menus 100% configurables** en `messages.yml` y `menus.yml`.
- **Multi-idioma**: `settings.language` selecciona el idioma y se anaden
  `messages_en.yml` y `menus_en.yml` (espanol e ingles incluidos; anadir otro
  idioma es copiar los textos a `messages_XX.yml` y `menus_XX.yml`).
- **PlaceholderAPI opcional**: placeholders `%jpsk_...%` para scoreboards,
  hologramas y tablists (`traders`, `exists`, `name`, `world`, `pages`,
  `trades`, `cooldown`, `ready`, coordenadas...).
- Licencia **MIT** y changelog publico.

### Corregido antes de la publicacion

- **1.8**: el clic derecho sobre el NPC no se detectaba en las versiones
  antiguas, porque alli el evento de interaccion tiene su propia lista de
  escuchadores. Ahora se atienden los dos eventos.
- **Duplicacion de items**: el editor permitia pedir hasta 64 unidades de
  cualquier item, incluidas espadas y pociones (que se apilan de una en una).
  Ahora la cantidad se limita a la pila real del material, tambien al cargar
  datos ya guardados.
- La comprobacion de inventario lleno (`trades.drop-result-if-full: false`)
  copiaba los 41 slots del inventario del jugador en un inventario de 36, lo
  que lanzaba una excepcion en 1.9+.
- **Materiales con data**: los alias de una misma familia se pisaban entre si
  (`STAINED_GLASS_PANE` acababa resolviendose siempre como blanco) y el
  formato antiguo `NOMBRE:data` no se reconocia en servidores 1.13+. Ahora se
  aceptan las tres formas en cualquier version.
- Un precio escrito como `NaN` se guardaba como 0 y el tradeo quedaba gratis
  sin avisar; ahora se rechaza.
- Configurar dinero, permiso o enfriamiento de un tradeo todavia vacio perdia
  lo escrito, porque al cerrar el inventario se borraba el tradeo. Ya no se
  borra mientras hay una entrada de chat pendiente, y las entradas caducan a
  los 2 minutos para no comerse el siguiente mensaje del jugador.
- El archivo del trader se escribia directamente sobre el original: un fallo a
  mitad de escritura lo dejaba truncado. Ahora se escribe en un temporal y se
  renombra de forma atomica.
- Un tradeo ya eliminado seguia permitiendo comerciar desde un menu abierto;
  ahora se revalida antes de ejecutar.
- Los enfriamientos por jugador se acumulaban en memoria sin limite; ahora se
  purgan al crecer.
- Al arrancar se adoptaban todos los NPCs en un solo tick (cargando un chunk
  por trader); ahora se reparten uno por tick.
- Si en la carpeta de traders habia dos archivos con el mismo id en distinta
  mayuscula, se fundian en uno; ahora se avisa y se carga solo el primero.

### Corregido en la revision previa a la publicacion (segunda pasada)

- **Nombres de material en las ultimas versiones**: alli `Material#name()` puede
  devolver nombres recortados o lanzar una excepcion (comprobado en Paper 26.3:
  `GRAY_STAINED_GLASS_PANE` devolvia `AINED_GLASS_PANE` y `STONE` lanzaba
  `StringIndexOutOfBoundsException`). Los menus y los mensajes usan ahora
  `Materials.nameOf`, que prefiere la clave del registro y nunca lanza.
- **El coste ya no se cobra de la armadura**: en 1.9+ el inventario del jugador
  incluye armadura y mano secundaria (41 slots) y el conteo/retirada de items
  los recorria, asi que un peto igual al coste se podia entregar (y se llevaba
  puesto). Ahora solo se miran los 36 slots de almacenamiento.
- **NBT en 1.8**: el tag normalizado solo quitaba `Enchantments` (nombre de
  1.13+), de modo que en 1.8 cualquier item encantado contaba como "NBT propio"
  y exigia un NBT identico al 100% (incluido `RepairCost`). Se quitan tambien
  `ench`, `RepairCost` y `Damage` (la durabilidad la decide su propio flag).
- **Sin IA y silencio en 1.8**: ese NMS no tiene `setNoAI` ni `setSilent`
  (verificado con javap sobre `patched_1.8.8.jar`), asi que el NPC conservaba IA
  y sonidos. Ahora se escriben las etiquetas `NoAI` y `Silent` en el NBT del mob.
- **PDC con arrays**: `String.valueOf` de un array devuelve su identidad
  (`[B@1a2b3c`), distinta en cada item, asi que un coste con un array en el PDC
  (1.14+) no se podia pagar nunca. Ahora se compara por contenido.
- **Fuga de UUID de entidades**: si el NPC moria o su chunk estaba descargado,
  su UUID se quedaba en el mapa de proteccion para siempre.
- **Rendimiento**: el analisis NBT del coste se hacia por cada slot del
  inventario y `Reflect` resolvia los metodos con `getMethods()` en cada
  llamada; ahora se hace una vez por consulta y los metodos se cachean
  (repintado de un menu de 28 iconos: de ~10,6 ms a ~1,5 ms en 1.8.8).
- **Archivos del trader con mayusculas**: se normalizan a minusculas al cargar,
  en vez de dejar un archivo congelado o perder lo editado.
- **Adopciones en cola**: no resucitan un trader eliminado ni recrean su archivo.
- **Editores abiertos tras `/trader reload`**: sus cambios se aplican al trader
  vivo en vez de reescribir el archivo con datos viejos.
- **Chat**: dos mensajes en el mismo tick con un prompt pendiente ya no se
  cancelan los dos; la entrada se reclama de forma atomica y caduca sola.
- **Traduccion incompleta**: las claves que falten en un `messages_XX.yml` o
  `menus_XX.yml` se toman del archivo base y la consola dice que archivo se uso.
- **Economia**: un proveedor de Vault que falle ya no deja al jugador sin menu.
- **Repintado de menus**: un error al pintar un menu ya no impide que se abra.
- `/trader reload` reevalua el hook de PlaceholderAPI y los slots fuera del grid
  del placeholder de enfriamiento ya no devuelven el tradeo de otra posicion.
- **1.8: los NPCs no quedaban sin IA ni silenciosos**. Ese NMS no tiene
  `setNoAI` ni `setSilent` (verificado con javap sobre `patched_1.8.8.jar`),
  asi que ahora se escriben las etiquetas `NoAI` y `Silent` del mob y se
  recargan. Comprobado en un servidor 1.8.8 real: el NPC sale con ambas.
- **Red de seguridad con el comercio vanilla**: si la ventana de comercio del
  aldeano llegara a abrirse sobre un NPC del plugin, se cancela y se abre el
  menu en su lugar (el comercio de aldeanos normales no se toca).

### Funciones de versiones modernas

- **MiniMessage opcional** (`settings.mini-message`): etiquetas como `<red>` o
  `<gradient:#00ff00:#0088ff>` en Paper 1.16+, resueltas con Adventure por
  reflexion. Los codigos `&` y `&#RRGGBB` siguen funcionando a la vez y en
  Spigot la opcion no tiene efecto (con aviso en consola).
- **Chat moderno de Paper**: ademas de `AsyncPlayerChatEvent` se escucha
  `AsyncChatEvent` (registrado por reflexion solo si existe), para que la
  edicion por chat siga funcionando si algun dia retiran el evento antiguo.
- **Cooldowns persistentes**: los enfriamientos activos se guardan en
  `cooldowns.yml` al apagar el servidor y se recuperan al arrancar.
- **Deteccion de Folia**: el plugin avisa y se desactiva en Folia en vez de
  fallar de forma imprevisible (su planificador por regiones no esta soportado).

- **Colores hexadecimales `&#RRGGBB`** en todos los textos y menus. En 1.16+ se
  usa el color nativo; en versiones anteriores se aproxima al color legacy mas
  parecido, asi que un mismo archivo vale en cualquier servidor.
- **`custom-model-data`** en los iconos de `menus.yml` (1.14+): texturas de
  resource pack en los botones, ignorado en versiones que no lo tienen.
- **Cabezas de jugador** en los iconos (`head: Nombre`), sin consultas externas:
  usa jugadores en linea o ya conocidos por el servidor.
- **Titulos de menu largos** en 1.13+: el limite de 32 caracteres solo se aplica
  en clientes antiguos.
- **Mobs de versiones modernas** en `npc.allowed-types`; los que no existen en
  el servidor se rechazan con aviso y el boton "Tipo de mob" los salta en vez
  de dejar el trader sin NPC.

### Pruebas

- 18 pruebas automaticas (`mvn test`) sobre la logica pura: resolucion de
  materiales entre versiones, validacion de numeros y cantidades de los
  tradeos. Se ejecutan tambien en `mvn package`.

### Notas

- Probado en **Paper 1.8.8** y **Paper 1.21.11** (arranque, creacion y
  adopcion de NPCs, edicion de tradeos con items, comandos, recarga y
  apagado limpio).
- Vault y PlaceholderAPI son **opcionales**: sin ellos el plugin funciona
  igual (solo se desactivan el dinero y los placeholders).
