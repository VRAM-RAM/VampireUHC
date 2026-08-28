# Roles (incomplet)

Les rôles ne doivent pas simplement révéler des informations directes.

Le but est l'interprétation.

Progression : ~70 % (15 rôles villageois, 3 rôles vampire soit 6 joueurs vampires, 3 rôles solitaires, et un rôle hybride => 24 joueurs).
Il manque donc 1 rôle villageois.

## Sommaire

* [Vampires](#vampires)
  * [Maître Vampire](#maître-vampire)
  * [Sbire Vampire](#sbire-vampire)
  * [Le Comte](#le-comte)

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
  * [Baba Yaga](#baba-yaga)
  * [Banshee](#banshee)
  * [Le Veilleur](#le-veilleur)
  * [L'Exorciste](#lexorciste)
  * [Le Prêtre](#le-prêtre)
  * [Le Bourreau](#le-bourreau)

* [Solitaires](#solitaires)
  * [Apprentie assassin](#apprentie-assassin)
  * [Gremlin](#gremlin)
  * [Doppelganger](#doppelganger)

* [Hybrides](#hybrides)
  * [Dame Blanche](#dame-blanche)



## Vampires

Les vampires constituent le camp minoritaire. Ils affaiblissent le village tout en gagnant de la force, et en évitant de se faire démasquer. Pour gagner, ils doivent éliminer tous les non-vampires.

### MAÎTRE VAMPIRE

Le Maître est le chef vampire.

Il possède des marqueurs spécifiques : les marqueurs Maître.

Les marqueurs Maître ont une aura obscure.

À chaque début d'épisode, le Maître peut placer un marqueur Maître sur un joueur non-vampire qu'il a croisé durant l'épisode précédent. (peut etre plus frequemment, on verra pour l'équilibrage).

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

Pour pouvoir voter pour un joueur, ils doivent l'avoir croisé durant l'épisode précédent.

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

### Le Comte

Le **Comte** est un vampire puissant.
Il vote, comme les sbires, pour attribuer la marque vampire.
À chaque debut d'épisode, il reçoit le nombre de personnes ayant une aura lumineuse proche de lui (dans un rayon de 50 blocs autour de lui).

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

> [!NOTE]
>  Le Marchand de sable peut s'ensabler lui-même, mais ne peut ensabler un joueur qu'une seule fois dans la partie.

À la mort du Marchand de Sable, toutes les personnes possédant un marqueur sable subissent un effet de blindness pendant 30 secondes, et un effet de lenteur pendant 3 minutes.

> [!WARNING]
> L'aura d'un marqueur est fixe. Si, par exemple, Bob place un marqueur Sable sur Alice qui est villageoise, le marqueur prendra une aura lumineuse. Si le gremlin switch les marqueurs d'Alice et Majory, qui est vampire, ce dernier recevra le marqueur sable avec une aura lumineuse et non neutre.

### Archer

L'archer est un rôle villageois pur pvp.
À l'annonce des rôles, il reçoit un arc *infinity* et *unbreaking III*. 
Il possède également un pouvoir passif : lorsqu'il tire et atteint quelqu'un, cette personne obtient un effet de glowing (visible seulement par l'archer) durant **15** secondes.

### Baba Yaga 

**Baba Yaga** possède deux pouvoirs : sa résurrection et sa malédiction.
\
\
*Résurrection (à usage unique)* :
\
Lorsqu'un joueur meurt, elle reçoit un message cliquable qui lui permet de le ressusciter.
Si elle le fait, le joueur ressuscite avec tous ses pouvoirs et doit toujours gagner avec son camp, mais :

- s'il était un vampire : Baba Yaga subit un effet léger (et invisible à ses yeux) de faiblesse permanente
- si Baba Yaga meurt, la personne qu'elle a ressuscité meurt également (quel que soit son camp), sauf si c'est elle qui l'a tuée !

\
*Malédiction (à usage unique)* :
\
À l'aide la commande `/vuhc maudire <joueur>`, Baba Yaga peut maudire un joueur. Ce dernier voit ses pommes d'or ne lui conférer aucun coeur d'absorption pendant *3* minutes.

### Banshee

La **Banshee** est un rôle villageois à info qui s'apparente au Montreur d'Ours en loup-garou UHC.
À chaque début d'épisode, un message est envoyé à tous les joueurs. Le contenu de ce message dépend du nombre d'auras obscures qui se trouvent dans un rayon de *50* blocs autour d'elle.

- Aucune aura obscure : Pas de message
- 1-2 auras obscures : `La Banshee pleure...`
- 3+ : `La Banshee pousse un cri effrayant !`


### Le Veilleur

Le **veilleur** est un rôle villageois à information mineure. Il peut, à chaque épisode, inspecter aléatoirement un des marqueurs que possède le joueur ciblé (se trouvant dans un rayon de 20 blocs autour de lui), à l'aide de la commande :
\
```mc
/vuhc veiller <joueur>
```

Il ne peut pas *veiller* le même joueur deux fois de suite, et peut se *veiller* lui-même.

### L'Exorciste

L'**Exorciste** est un rôle villageois puissant. A chaque épisode, à l'aide de la commande `/vuhc exorciser <Joueur>`, il peut supprimer l'ensemble des marqueurs obscurs d'un joueur, et connaître l'ensemble des marqueurs obscurs que possédait le joueur (utilisable une seule fois par joueur au cours de la partie).

### Le Prêtre

Le **prêtre** est un rôle villageois à info majeur. À chaque épisode, il peut exécuter la commande `/vuhc percevoir <joueur>`, qui lui révèle l'aura exacte du joueur ciblé. (utilisable plusieurs fois sur le même joueurs, mais pas deux fois de suite sur le même joueur).
\
Attention cependant : plus l'aura du **Prêtre** est obscure, plus la probabilité que sa perception soit fausse augmente :

- aura très lumineuse : 0 %
- aura lumineuse : 5 %
- aura neutre : 10 % 
- aura obscure : 25 %
- aura très obscure : 40 %

### Le Bourreau

Le **bourreau** est un rôle villageois mineur, pvp pur (à la manière de l'archer). 
À l'annonce des rôles, il reçoit un livre *sharpness II*. 
À chaque épisode, le premier coup qu'il met à un joueur inflige 50% de dégats en plus.

## Solitaires

Les rôles solitaires gagnent tout seuls. Ils doivent éliminer l'intégralité de la partie.

### Apprentie assassin

L'apprentie assassin est l'un des rôles solitaire. Son but ? Gagner seule, en éliminant l'ensemble des autres joueurs. Pour ce faire, elle possède deux pouvoirs :

1. À chaque kill qu'elle prend, l'apprentie assassin récupère les marques (sauf les marqueurs maîtres, pour éviter une infection obligatoire) du joueur tué. 
2. En fonction des marques qu'elle possède, ses pouvoirs varient (et sont cumulatifs) :
    - Plus de X marqueurs obscurs --> Force légère la nuit
    - Plus de X marqueurs lumineux --> Force légère le jour
    - Plus de nX marqueurs obscurs --> Force la nuit & Régénération naturelle d'un demi-coeur par minute la nuit.
    - Plus de nX marqueurs lumineux --> Force le jour & Régénération naturelle d'un demi-coeur par minute le jour.
    
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

### Doppelganger

Le Doppelganger est un rôle solitaire. Son but ? Gagner seul, en empruntant la force des autres — et en punissant sévèrement quiconque lui prend sa nouvelle identité.

Entre 20 et 60 minutes de jeu, il peut, une seule fois, exécuter la commande :

```mc
/vuhc usurper <joueur>
```

Il copie alors l'ensemble des pouvoirs actifs et passifs du joueur ciblé, et ce jusqu'à la mort de ce dernier.

#### Fonctionnement de l'usurpation

Le Doppelganger dispose de son propre pool d'usages, totalement indépendant de celui du joueur copié : il ne consomme pas les charges de l'original, et l'original conserve les siennes intactes.

Quelques précisions selon le rôle copié :

- **Rôle avec objet unique** (Archer, Bourreau...) : le Doppelganger reçoit uniquement l'effet passif du rôle (glowing au tir, bonus de dégâts au premier coup...), jamais l'objet physique associé (arc, livre...).
- **Maître Vampire** : le Doppelganger peut poser ses propres marqueurs Maître (aura obscure, pénalité d'absorption à 2 marqueurs), mais ces marqueurs n'entraînent jamais d'infection, quel que soit leur nombre. Il ne bénéficie pas non plus du malus de vie du Maître (~8 coeurs).
- **Sbire Vampire** : le Doppelganger peut voter pour la marque vampire, mais ne reçoit jamais la liste des alliés vampires à 45 minutes.
- **Cupidon** : le Doppelganger ne connaît pas les couples déjà formés par le Cupidon original, mais s'il se produit un changement de marqueur Amour après son usurpation (switch, etc.), il en est notifié comme le serait le vrai Cupidon.
- **Tisseur** : le Doppelganger doit tisser son propre réseau depuis zéro ; il n'hérite pas de celui du Tisseur original.
- **Banshee** : le message de la Banshee copiée n'est envoyé qu'au Doppelganger, jamais à l'original ni à qui que ce soit d'autre.

En résumé : le Doppelganger copie le *pouvoir*, jamais l'*état* ou les *informations déjà accumulées* du joueur usurpé.

> [!NOTE]
> Le pouvoir copié est parfois **nerf** par rapport au pouvoir original, pour éviter que le **Doppelganger** devienne un rôle beaucoup trop puissant.

#### Mort du joueur usurpé

Si le joueur usurpé meurt :

- Le Doppelganger perd immédiatement tous les pouvoirs copiés, ainsi que tous les marqueurs qu'il a lui-même posés en tant que ce rôle.
- Il reçoit l'identité du tueur.
- Les dégâts qu'il inflige à ce tueur sont augmentés de **50%**, de façon permanente, jusqu'à la fin de la partie.
- S'il parvient à tuer ce tueur, il gagne un effet **force + speed permanent**.

S'il ne parvient jamais à atteindre ou identifier le tueur (mort avant lui, par exemple), le bonus de dégâts reste acquis mais n'aura simplement jamais d'occasion de s'exprimer — tant pis pour lui.

#### Exemple

À 35 minutes, le Doppelganger usurpe Alice, qui est Prêtre. Il peut désormais percevoir l'aura des joueurs comme elle le ferait, avec son propre pool d'usages.

À 1h20, Alice se fait tuer par Bob. Le Doppelganger perd instantanément son pouvoir de perception et tous les marqueurs qu'il avait lui-même posés en tant que Prêtre usurpé, mais reçoit l'identité de Bob comme tueur. Dès lors, tous ses coups portés contre Bob infligent 50% de dégâts supplémentaires. S'il parvient à l'achever, il gagne un effet de force et de vitesse pour le reste de la partie.
\
\
Pour plus d'informations à propos de l'usurpation, vous pouvez consulter [la doc du doppelganger](./Doppelganger.md).

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
