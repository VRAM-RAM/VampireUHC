# VampireUHC

VampireUHC est un mode de jeu minecraft inspiré de Loup-Garou UHC.
\
\
🔴 **Agent-assisted**

> [!NOTE]
> This small project is entirely in french. You can contribute it if you don't speak french anyway. Also, this is my first Java project (I learn Java by coding it) so it may not be really efficient nor idiomatic. I used agentic (Opencode : Big Pickle) to complete some 'plumbing' (such as `serialization`). Anyway, all ideas and the `minecraft-ish` code are from me.  
>
> Ce petit projet - entièrement en français - est mon premier projet Java, qui me permet d'apprendre le langage (pour cette raison, il n'est ni efficace ni idiomatique). J'ai utilisé de l'agentique (OpenCode : Big Pickle) pour faire de la 'plomberie' (la `serialization`, par exemple). Cependant, toutes les idées viennent de moi, ainsi que tout le code lié à MineCraft (les rôles, les events...).

## Sommaire

* [Concept](#concept)
* [Déroulement du mode de jeu](#déroulement-du-mode-de-jeu)
* [Les marqueurs](#les-marqueurs)
* [Les auras](#les-auras)
* [Composition](#compo)
  * [Les vampires](#vampires)
  * [Les marques vampires](#les-marques-vampires)
* [Objectif de gameplay](#objectif-de-gameplay)
* [Rôles](#rôles)
* [License](#license)


## Concept

Les joueurs, à 20 minutes de jeu, reçoivent un rôle qui determine leur but dans la suite de la partie, et leur pouvoirs.
Il existe trois camps :

- Les vampires (antagonistes du mode de jeu) : Ils sont minoritaires et doivent éliminer tous les non-vampires. Leur force augmente progressivement grâce aux marqueurs.
- Les villageois (protagonistes) : Ils gagnent ensemble. Ils doivent éliminer les vampires et les rôles solitaires.
- Les rôles solitaires : Ils gagnent seuls / en duo, en étant les derniers survivants.


L'ensemble du mode de jeu se construit autour de la mécanique des *marqueurs*.

## Déroulement du mode de jeu

- 0 --> 20 minutes : les joueurs commencent à miner, préparer keur stuff.
- 20 minutes : annonce des rôles. 
- 20 --> 45 minutes : les joueurs continuent le minage, le pvp n'est pas activé, mais la stratégie commence : le salvateur pose sa première marque, le cupidon pose ses marques...
- 45 --> fin de la partie : phase de pvp et de jeu. Les vampires reçoivent la liste de leurs alliés.
- fin de la partie : il ne reste qu'un camp en jeu

## Les marqueurs

Les marqueurs sont le coeur du gameplay. Chaque joueur peut posséder plusieurs marqueurs, qui peuvent :

- modifier l'aura d'un joueur
- modifier ses pouvoirs
- donner des informations indirectes
- créer des stratégies d'enquête

Pour l'instant, voici les marqueurs prévus :

- Marqueurs Amours : donné par le cupidon à deux joueurs. Si l'un des deux joueurs ayant un marqueur amour meurt, l'autre perd 5 coeurs permanents pendant 10 minutes et reçoit l'identité du tueur. Ont une aura neutre.
- Marqueur Salvation : donné par le salvateur à chaque épisode (ne peut pas être donné deux fois consécutives au même joueur). Protège contre les marques vampires et maitre. Par exemple, si Alice reçoit la marque Salvation puis la marque vampire, la marque Salvation disparaît et la marque Vampire n'est pas appliquée. A une aura lumineuse.
- Marqueurs Vampire : donnés par les vampires (le résultat de leur vote). Ont une aura obscure.
- Marqueurs Maitre : donnés par le maitre. A une aura obscure.
- Marqueurs Lumineux : peuvent être donnés par divers moyens. Ont une aura lumineuse.

## Les auras

Chaque marqueur possède une aura :

- Obscure.
- Neutre.
- Lumineuse.

L'aura globale d'un joueur dépend du ratio entre ses marqueurs. Elle peut être :

- très obscure
- obscure
- neutre
- lumineuse
- très lumineuse

Elle n'indique pas directement de camp, mais permet de donner des indices (exemple : au premier vote vampire, Alice voit son aura changer de neutre à obscure : elle a donc reçu la marque Vampire. Or, les vampires ne pouvant pas recevoir leur propre marque, il y a de grandes chances qu'Alice soit villageoise (ou solo). En revanche, le gremlin peut avoir échangé les marques de Bob (villageois) avec Alice (vampire), qui reçoit alors la marque vampire... alors qu'elle est vampire.)

## Compo

Composition envisagée pour environ 24 joueurs :
- 6 à 7 vampires maximum.
- 15 à 16 villageois.
- 2 à 3 solitaires. (dont le traire vampire qui apparait comme étant vampire)

### Vampires

Les vampires sont très minoritaires.

Avant 45 minutes :
- Ils ne se connaissent pas.
- Ils n'ont pas de chat privé.
- Ils doivent agir seuls.

À 45 minutes :
- Ils découvrent la liste des autres vampires.
- Ils peuvent commencer à coopérer, mais sans se faire cramer.

Les vampires ont au début :
- Une faiblesse pendant le jour.

Grâce aux marques vampires :
- Ils peuvent supprimer cette faiblesse.
- Ils peuvent gagner de la force pendant la nuit.

Les seuils dépendent du nombre de joueurs dans la partie.
Ils votent séparément pour la marque, mais les votes sont mis en commun. En cas d'égalité, le Maitre tranche.

### Les marques vampires

Les vampires votent pour attribuer des marqueurs vampires à des joueurs non-vampires.

Le vote est fait sans communication directe.

Ils reçoivent uniquement le résultat de leur vote :
"Votre cible est Bob."

Ils ne savent pas :
- si Bob a réellement reçu la marque (protégé par exemple)
- si une autre mécanique a empêché l'action.

Les marques vampires :
- ont une aura obscure.
- servent à affaiblir les joueurs villageois (qui ont souvent des pouvoirs liés à une aura lumineuse).
- servent aussi à faire progresser le camp vampire.

Exemple :
Un rôle dépendant de l'aura peut devenir plus faible à cause des marques vampires.

## Objectif de gameplay

Vampire UHC doit être un jeu où :

- Personne ne possède toute la vérité.
- Les pouvoirs donnent des indices indirects.
- Les joueurs doivent reconstruire les événements.
- Les auras donnent des informations ambiguës.
- Les marqueurs créent une histoire invisible de la partie.

Le but n'est pas seulement de trouver les vampires.

Le but est de comprendre ce qui s'est passé à partir des conséquences :
- changements de pouvoirs,
- auras,
- morts,
- protections,
- comportements.

## Rôles

Pour les rôles, lire [ROLES.md](./doc/ROLES.md).

## License

VampireUHC is licensed under [MIT licence](./LICENSE-MIT) and [CECILL licence](./LICENSE-CECILL-EN).

VampireUHC est licensié sous les licenses [MIT](./LICENSE-MIT) et [CECILL](./LICENSE-CECILL-FR).