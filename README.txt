=== SOUBORY ===



Přiložené soubory (všechny v kořenovém adresáři projektu):

- uml.png: diagram tříd

- demo.txt: Uložená mapa s dostatečným počtem křižovatek a cest, kterou lze otevřít v aplikaci

- block_file: Blokový seřazený soubor s 5000 vygenerovanými křižovatkami s ID od aaa, aab, aac, ..., hkg, hkh.



Vstupní soubor

- src/gui/Main




=== Ovládání z GUI ===



Práce s mapou

- otevření nové mapy: Soubor -> Nový -> Prázdný

- vygenerování náhodné mapy: Soubor -> Nový -> Vygenerovat...

- otevření mapy ze souboru: Soubor -> Otevřít...

- uložení mapy do souboru: Soubor -> Uložit...



Editace křižovatek a cest

- přidání křižovatky/cesty: Tlačítko Add vpravo v sekci Oblasti (resp. Cesty)

- úprava/smazání křižovatky/cesty: Tlačítka Edit/Delete vpravo v sekci Oblasti (resp. Cesty). Nejdříve je nutno zakliknout křižovatku/cestu v seznamu

- aktivovat/deaktivovat všechny cesty v oblasti: Výběr -> Stav cest -> Aktivovat (resp. Deaktivovat)

- smazat všechny křižovatky/cesty v oblasti: Výběr -> Smazat křižovatky (resp. Smazat cesty)



Vyhledávání a výběr

- křižovatka podle ID: Najít -> Křižovatku -> Podle ID...

- křižovatka podle souřadnic: Najít -> Křižovatku -> Podle souřadnic

- nejkratší cesta mezi 2 body: Najít -> Cestu...

- vybrat všechny křižovatky a cesty v oblasti: Stisknout myš a tažením vybrat požadovanou oblast



Blokový soubor

- vytvoření nového blokového souboru: Soubor -> Nový -> Blokový soubor...

- otevření existujícího blokového souboru: Soubor -> Otevřít -> Blokový soubor...

- Vygenerování blokového souboru (v okně blokového souboru): Soubor -> Vygenerovat...

- Naleznutí křižovatky (v okně blokového souboru): Křižovatka -> Najít -> Interpolačně...

- Smazání křižovatky (v okně blokového souboru): Křižovatka -> Odstranit



Ostatní

- zobrazit směrovací matici: Zobrazit -> Směrovací matice

- zobrazit/skrýt mřížku: Zobrazit -> Mřížka

- zobrazit/skrýt legendu: Zobrazit -> Legenda

- zobrazit/skrýt popisky: Zobrazit -> Popisky
