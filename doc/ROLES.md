# Roles (incomplet)

Les rôles ne doivent pas simplement révéler des informations directes.

Le but est l'interprétation.

Progression : ~55 % (9 rôles villageois, 2 rôles vampire soit 5 joueurs vampires, 2 rôles solitaires => 16 joueurs).
Il manque donc 7-9 rôles villageois, un rôles solitaire et un rôles vampires (peut être le traitre vampire?!).

## Sommaire

* [Vampires](#vampires)
  * [Maître Vampire](#maître-vampire)
  * [Sbire Vampire](#sbire-vampire)

* [Villageois](#villageois)
  * [Salvateur](#salvateur)
  * [Cupidon](#cupidon)
  * [Paladin](#paladin)
  * [La Peseuse d'Âmes](#la-peseuse-dâmes)
  * [Le Fossoyeur](#fossoyeur)
  * [Tisseur](#tisseur)
  * [Cartographe](#cartographe)
  * [Marchand de Sable](#marchand-de-sable)
  * [Archer](#archer)

* [Solitaires](#solitaires)
  * [Apprentie assassin](#apprentie-assassin)
  * [Gremlin](#gremlin)

* [Hybrides](#hybrides)
  * [Dame Blanche](#dame-blanche)



## Vampires

Les vampires constituent le camp minoritaire. Ils affaiblissent le village tout en gagnant de la force, et en évitant de se faire démasquer. Pour gagner, ils doivent éliminer tous les non-vampires.

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

## Villageois

Les villageois sont majoritaire dans la partie. Leur but ? Démasquer tous les traîtres et les éliminer.

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

### La Peseuse d'Âmes

La peseuse d'âmes est un rôle à info mineur.
À chaque épisode, à l'aide de la commande `/vuhc peser <Joueur1> <Joueur2>`, elle peut *peser* l'aura de deux joueurs.
S'offrent alors trois possibilités de résultats :

- `La balance s'équilibre...` => les deux joueurs ont exactement la même aura (très obscure, obscure, neutre...).
- `La balance penche légèrement...` => les deux joueurs ont une aura de la même catégorie (obscure, neutre ou lumineuse).
- `La balance penche...` => les deux joueurs ont une aura de catégorie différente

### Fossoyeur

Le fossoyeur est un rôle à info moyen, qui n'agit pas en early game, mais post-mortem d'un joueur :

En se rendant sur le cadavre d'un joueur mort (qu'il voit entouré de particules **rouges**), il peut executer la commande `/vuhc exhumer`, et reçoit alors la liste des marqueurs détenus par le défunt.

### Tisseur

Le Tisseur est un rôle villageois à information moyenne, qui agit principalement en milieu et fin de partie. Son pouvoir ne révèle jamais directement des rôles : il lui permet d'observer les liens entre plusieurs joueurs et d'interpréter les événements qui s'y produisent.

À l'aide de la commande :

```mc
/vuhc tisser <Joueur>
```

le Tisseur peut intégrer un joueur qui se trouve à moins de 20 blocs autour de lui à son réseau.

Il ne peut utiliser cette commande que sur un joueur proche de lui.

Le Tisseur doit tisser un réseau de **3 à 4 joueurs**. Tant que son réseau contient moins de 3 personnes, il reste incomplet et ne produit aucun effet.

Une fois le réseau constitué, le Tisseur n'obtient aucune information immédiate. Il doit attendre que des événements se produisent parmi les joueurs qu'il observe.

#### Mort d'un joueur du réseau

Si l'un des joueurs du réseau meurt, le Tisseur reçoit :

* le pseudo du joueur mort ;
* son aura exacte au moment de sa mort.

Le réseau s'effondre alors entièrement.

Le Tisseur doit ensuite reconstruire un nouveau réseau en utilisant à nouveau sa commande.

#### Meurtres dans le réseau

Si un joueur du réseau est crédité d'une élimination, le Tisseur reçoit une notification :

```text
Un noeud de votre réseau a assassiné.
```

Le réseau ne s'effondre pas après un meurtre.

### Cartographe

Le cartographe est un rôle à info mineur.

À l'aide de la commande :
```mc
/vuhc baliser
```
le cartographe place une balise à l'endroit où il se trouve.
Durant l'épisode et l'épisode suivant, tout passage de joueur est enregistré par la balise et communiqué au début de l'épisode suivant au cartographe.
À chaque épisode, il peut repositionner sa balise en réutilisant sa commande.
La balise, après avoir passé 2 épisodes au même endroit, disparaît automatiquement, forçant le cartographe à en replacer une.

### Marchand de Sable

Le Marchand de Sable est un rôle de soutien léger.
Chaque épisode, il peut, à l'aide de la commande `/vuhc ensabler <joueur>`, déposer un marqueur **sable** sur un joueur de son choix qui se trouve dans un rayon de 10 blocs autour de lui. Si le joueur ciblé est villageois, le marqueur prendra une aura lumineuse. Sinon, il prendra une aura neutre.

> [!NOTE] Le Marchand de sable peut s'ensabler lui-même, mais ne peut ensabler un joueur qu'une seule fois dans la partie.

À la mort du Marchand de Sable, toutes les personnes possédant un marqueur sable subissent un effet de blindness pendant 30 secondes, et un effet de lenteur pendant 3 minutes.

> [!WARNING] L'aura d'un marqueur est fixe. Si, par exemple, Bob place un marqueur Sable sur Alice qui est villageoise, le marqueur prendra une aura lumineuse. Si le gremlin switch les marqueurs d'Alice et Majory, qui est vampire, ce dernier recevra le marqueur sable avec une aura lumineuse et non neutre.

#### Archer

L'archer est un rôle villageois pur pvp.
À l'annonce des rôles, il reçoit un arc *infinity* et *unbreaking III*. 
Il possède également un pouvoir passif : lorsqu'il tire et atteint quelqu'un, cette personne obtient un effet de glowing (visible seulement par l'archer) durant **15** secondes.

## Solitaires

Les rôles solitaires gagnent tout seuls. Ils doivent éliminer l'intégralité de la partie.

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

Le Gremlin est un autre rôle solitaire. Il a pour pouvoir de manipuler les marques et de voler la vie des ses adversaires.

Son premier pouvoir est donc, à chaque épisode, de pouvoir `switch` (échanger) l'ensemble des marques de deux joueurs, via la commande :
```mc
/vuhc switch <joueur1> <joueur2>
```

Cela peut être intéressant pour semer la zizanie, handicaper des rôles villageois voire retarder l'infection du maître. Le Gremlin peut s'auto-switch.
En revanche, le cupidon est notifié si une marque d'amour change de propriétaire, et les rôles à info pourront avoir des suspicions (changement d'aura...).

Son second pouvoir est de voler, en plein combat, la vie du joueur qu'il frappe : à chaque coup porté, X % de chance qu'il gagne un demi-coeur. 
Ce pouvoir est activable pendant 5 minutes à chaque épisode, à l'aide de la commande :
```mc
/vuhc drain
```

À la fin des 5 minutes, il subit un malus temporaire léger (poison léger et court).

## Hybrides

### Dame Blanche

La Dame Blanche est un rôle hybride. À l'annonce des rôles, elle appartient au village et n'a aucun pouvoir particulier. 
En revanche, les conditions de sa mort determinent le reste de sa partie :

- Si elle est tuée par un *villageois*, elle ressuscite et devient solitaire.
- Si elle est tuée par un *vampire*, elle ressuscite et doit toujours gagner avec les villageois.
- Si elle est tuée par un *rôle solitaire*, elle meurt définitivement.

Elle ne peut ressusciter qu'une seule fois.

Si elle devient solitaire, elle obtient un effet **résistance** de nuit et **force** de jour, ainsi qu'un effet de **speed** permanente si elle tue son meurtrier.
\
Si elle est tuée par un vampire, elle obtient l'effet **faiblesse** de jour.