# VampireUHC

VampireUHC est un mode de jeu minecraft inspiré de Loup-Garou UHC.
\
\
🔴 **Agent-assisted**

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

## Les marques vampires

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

## Roles (incomplet)

Les rôles ne doivent pas simplement révéler des informations directes.

Le but est l'interprétation.

Les catégories principales :

Village :
- Information pure.
- Mix information/pouvoir (catégorie principale).

Vampires :
- Sbires vampires. 
- Maître.
- Vampire spécial. (pour grosse compo)
- Traître vampire (type Loup-Garou Blanc mais vampire).

Solitaires :
- Pas de marqueurs spécifiques.
- Mécaniques uniques.

### MAÎTRE VAMPIRE

Le Maître est le chef vampire.

Il possède des marqueurs spécifiques : les marqueurs Maître.

Les marqueurs Maître ont une aura obscure.

À chaque épisode, le Maître peut placer un marqueur Maître sur un joueur non-vampire. (peut etre plus frequemment, on verra pour l'équilibrage).

Effets :
- 1 marqueur Maître :
  Rien, mais le joueur porte une influence cachée.

- 2 marqueurs Maître :
  Le joueur reçoit moins d'efficacité avec les pommes d'or (un coeur d'absorption en moins lorsqu'il en mange une).

- 3 marqueurs Maître :
  Le joueur devient vampire (infecté).

Lorsqu'un joueur est infecté :
- Son infection est permanente.
- Les marqueurs Maitre de tous les joueurs disparaissent.
- Il rejoint le camp vampire, et gagne les pouvoirs des vampires.
- Il ne peut pas voter pour les futures marques vampires.

Le Maître est volontairement fragile :
- Environ 8 coeurs au lieu de 10.

### SALVATEUR

Le Salvateur est un rôle villageois.

À chaque épisode, il place un marqueur Salvation sur un joueur.

Ce marqueur possède une aura lumineuse.

Si le joueur ciblé par un vampire ou le Maître possède Salvation :
- La marque n'est pas appliquée.
- Les vampires ou le Maître pensent néanmoins que l'action a fonctionné.
- la marque Salvation disparaît

Le Salvateur crée donc de fausses informations, tout en protégeant ceux qu'il pense être safe.

### CUPIDON


Cupidon place au début de la partie un marqueur Amour sur deux joueurs.

Les joueurs ne le savent pas.

Effet :
- Les deux joueurs sont liés.
- Si l'un meurt, l'autre perd temporairement 5 coeurs permanents pendant 10 minutes.
- Il connaît l'identité du tueur.

Le marqueur Amour :
- possède une aura neutre.
- ne doit pas être supprimable facilement.

Cupidon gagne avec le village. Si jamais un/ les deux marqueur(s) amour changent de personne, le cupidon en est informé : dans un moment aléatoire entre 0 et 10 minutes après le changement, il reçoit une notification lui indiquant le(s) nouvel/(aux) amoureux.

### PALADIN

Le Paladin est rôle villageois dépendant de l'aura.

Effets :

Aura très obscure :
- perte d'un coeur.
- faiblesse légère.

Aura obscure :
- faiblesse légère (pas d'effet apparent, mais elle se ressent en combat : il suffit de masquer l'effet au joueur).

Aura neutre :
- aucun effet.

Aura lumineuse :
- force visible en combat (pas d'effet apparent).

Aura très lumineuse :
- force + deux coeurs supplémentaires. (peut etre 1, on verra pour l'équilibrage)

Le Paladin gagne une marque lumineuse lorsqu'il tue un vampire.

Cela permet des déductions :
- Il tue quelqu'un.
- Son pouvoir change.
- Il peut comprendre que la cible était probablement vampire.

Ou alors :
- Il perd un coeur -> il en déduit qu'il est tres obscur, donc qu'il a été ciblé. Il peut alors croiser ses infos avec celles d'autres rôles.

### Apprentie assassin

L'apprentie assassin est l'un des rôles solitaire. Son but ? Gagner seule, en éliminant l'ensemble des autres joueurs. Pour ce faire, elle possède deux pouvoirs :

1. À chaque kill qu'elle prend, l'apprentie assassin récupère les marques (sauf les marqueurs maîtres, pour éviter une infection obligatoire) du joueur tué. 
2. En fonction des marques qu'elle possède, ses pouvoirs varient (et sont cumulatifs) :
    - Plus de X marqueurs obscurs --> Force légère la nuit
    - Plus de X marqueurs lumineux --> Force légère le jour
    - Plus de nX marqueurs obscurs --> Force légère la nuit & Régénération naturelle d'un demi-coeur par minute la nuit.
    - Plus de nX marqueurs lumineux --> Force légère le jour & Régénération naturelle d'un demi-coeur par minute le jour.
    
Donc en fin de game, potentiellement T4 + Force perma + regen lente.

En revanche, l'assassin est vulnérable aux marqueurs maître qu'elle reçoit du maître : perte d'absorption partielle voire infection. Aussi, son aura varie énormément à chaque kill, donc peut être cramée / suspectée par les rôles à info.

### Gremlin

Le Gremlin est un autre rôle solitaire. Il a pour pouvoir de manipuler les marques et `<je n'ai pas encore décidé>`.

Son premier pouvoir est donc, à chaque épisode, de pouvoir `switch` (échanger) l'ensemble des marques de deux joueurs, via la commande :
```mc
/vuhc switch <joueur1> <joueur2>
```

Cela peut être intéressant pour semer la zizanie, handicaper des rôles villageois voire retarder l'infection du maître. Le Gremlin peut s'auto-switch.
En revanche, le cupidon est notifié si une marque d'amour change de propriétaire, et les rôles à info pourront avoir des suspicions (changement d'aura...).

Second pouvoir :

todo!

### Sbire Vampire

Les sbires vampires constituent le reste du camp vampire. En début de game, ils reçoivent l'effet faiblesse de jour. À 45 minutes de jeu, ils reçoivent la liste de leurs alliés, et peuvent commencer à voter pour attribuer des marques vampires (une toute les X minutes).

Ils votent pour une marque à l'aide de :
```mc
/vuhc voter <Joueur>
```

Leurs pouvoirs augmentent par nombre de personne marquées :

- X personnes --> perte de la faiblesse
- nX personnes --> gain de force la nuit

Ils peuvent attribuer leur marque plusieurs fois à la même personne, mais cela ne compte pas comme plusieurs joueurs marqués.
En cas d'égalité lors du vote, le Maître tranche.
En cas de `switch` des marques ou de mort d'une personne, le nombre de joueurs marqués ne change pas.

Par exemple :
Bob & Alice reçoivent une marque vampire chacun --> 2 personnes marquées.
Alice switch avec Majory (qui n'en avait aucune) --> toujours 2 personnes marquées.
Bob meurt --> toujours 2 personnes marquées.

Les vampires ne reçoivent que le résultat de leur vote :
```text
Vous avez marqué <Joueur> !
```

Ils ne savent pas si le marquage a fonctionné ou pas (salvateur?) et si les marques changent de propriétaire ou pas.
