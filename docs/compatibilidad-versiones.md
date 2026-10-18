# Funciones modernas: que hay, que falta y por que

Analisis de las cosas que se usan en servidores de Minecraft modernos y como
las cubre (o no) JPShopKeeper, que tiene que seguir funcionando desde la 1.8.

---

## 1. Lo que ya se usa (y desde que version)

| Funcion | Desde | Como se resuelve |
| --- | --- | --- |
| Materiales por nombre moderno y antiguo | 1.13 | `Materials.resolve` traduce `GRAY_STAINED_GLASS_PANE` <-> `STAINED_GLASS_PANE:7` |
| Sonidos por nombre, campo o clave de recurso | 1.9 | `Sounds` prueba la lista de candidatos de `config.yml` |
| `setAI`, `setSilent`, `setCollidable`, `setPersistent` | 1.9 / 1.14 | Reflexion con respaldo (en 1.8, etiquetas NBT del mob) |
| NBT de otros plugins | 1.8 - 1.20.4 | `NbtUtil` por reflexion sobre `getTag` |
| Componentes de item (sin `getTag`) | 1.20.5+ | `ItemStack.serializeAsBytes` + PDC |
| Titulos de inventario largos | 1.13+ | Se cortan a 32 solo en clientes antiguos |
| **Colores hexadecimales `&#RRGGBB`** | **1.16+** | **Nuevo**: se convierten al formato nativo; en versiones antiguas se aproximan al color legacy mas parecido |
| **`custom-model-data` en iconos de menu** | **1.14+** | **Nuevo**: `ItemBuilder.customModelData` (ignorado si no existe) |
| **Cabezas de jugador en iconos (`head:`)** | **1.8+** | **Nuevo**: `setOwningPlayer` (1.13+) o `setOwner` (1.8-1.12) |
| **Mobs de versiones modernas** | **1.14+** | **Nuevo**: en `npc.allowed-types`; los que no existen en el servidor se rechazan con aviso y el boton del editor los salta |
| **MiniMessage** (`<red>`, `<gradient>`) | **Paper 1.16+** | **Nuevo**: `settings.mini-message`; se resuelve por reflexion sobre Adventure y en Spigot no tiene efecto |
| **Chat moderno de Paper** (`AsyncChatEvent`) | **Paper 1.19+** | **Nuevo**: se registra solo si el evento existe; la edicion por chat sigue viva si retiran el antiguo |
| Vault (economia) | todas | Opcional |
| PlaceholderAPI | todas | Opcional |

---

## 2. Lo que NO tiene y por que

### 2.1 MiniMessage — **ya implementado (opcional)**
Se activa con `settings.mini-message: true`. En Paper 1.16+ las etiquetas se
convierten a formato legacy con Adventure (por reflexion); en Spigot, o si el
texto no es MiniMessage valido, se usa el texto tal cual. Los codigos `&` y
`&#RRGGBB` siguen funcionando a la vez, asi que no hay que reescribir textos.

### 2.2 Folia (hilos por region)
**Estado:** el plugin **detecta Folia**, avisa por consola y se desactiva en vez
de fallar de forma imprevisible.
**Por que no esta soportado:** usa el planificador de Bukkit (un solo hilo);
Folia exige `EntityScheduler`/`RegionScheduler` y prohibe tocar entidades de
otra region, asi que habria que reescribir todo el mantenimiento de NPCs y
probar en un servidor Folia real.
**Coste:** alto. **Cuando merece la pena:** si el recurso se vende a redes
grandes. Hoy Folia es marginal y la mayoria de plugins populares tampoco lo
soporta.

### 2.3 Comandos Brigadier (1.13+)
Nombres de argumento, sugerencias contextuales y un arbol de comandos real.
**Coste:** bajo-medio, pero **no aporta nada funcional**: el ejecutor legacy con
autocompletado funciona en todas las versiones, incluida la ultima.

### 2.4 `AsyncChatEvent` de Paper — **ya implementado**
El plugin escucha el evento moderno de Paper ademas del antiguo, sin compilar
contra Paper (se registra por reflexion solo si la clase existe). Si los dos
eventos llegan por el mismo mensaje, el segundo no lo procesa dos veces.

### 2.5 Entidades de visualizacion (`ItemDisplay`, `TextDisplay`, `Interaction`)
Permitirian NPCs 100% inmuebles sin IA ni colisiones.
**Por que no estan:** cambiarian el diseno (los NPCs son mobs nativos) y
romperian la 1.8. El anclaje por tick ya los deja clavados en el sitio.

### 2.6 Dialogos nativos (1.21.6+)
La API de dialogos de Paper sustituye a los menus de cofre para formularios.
**Por que no esta:** el plugin se basa en menus de cofre precisamente para
funcionar de la 1.8 a la ultima; los dialogos no existen antes de la 1.21.6.

### 2.7 Nombres de item con componentes (1.20.5+)
Desde la 1.20.5 un nombre de item puede llevar hover, click y degradados.
El plugin usa `setDisplayName(String)` legacy, que sigue funcionando y es lo
unico posible en 1.8.

### 2.8 Cooldowns persistentes — **ya implementado**
Los enfriamientos activos se guardan en `plugins/JPShopKeeper/cooldowns.yml`
al apagar el servidor y se recuperan al arrancar, en cualquier version.

---

## 3. Resumen para decidir

| Si tu publico es... | Que le falta |
| --- | --- |
| Spigot/Paper 1.8 - 1.12 | Nada: el plugin cubre todo lo que existe ahi |
| Paper 1.13 - 1.20 | Nada relevante: `&#RRGGBB`, `custom-model-data`, cabezas y chat moderno ya estan |
| Paper 1.21+ | Nada relevante: MiniMessage, hex, cabezas, PlaceholderAPI y cooldowns persistentes ya estan |
| Folia | El planificador por regiones: proyecto aparte (hoy se avisa y se desactiva) |
| Cualquiera | Brigadier y dialogos nativos no aportan a un menu de cofre y romperian la 1.8 |
