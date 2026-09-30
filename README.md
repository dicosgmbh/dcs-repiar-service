# Reparaturservice

Einer unserer Kunden betreibt eine Autowerkstatt. Auf seiner Homepage möchte er automatisierte Kostenvoranschläge für typische Reparaturen einbauen. Er hat bereits folgende Wünsche geäußert:

1. Meine Kunden sollen ihr Automodell und eine Liste von benötigten Reparaturen angeben können. Für die Kostenvoranschläge muss ich immer in der Tabelle price_estimations.csv nachsehen, welche Kosten im Durchschnitt erwartet werden. Das soll automatisch berechnet und dem Kunden angezeigt werden.
2. Ist der Kunde mit dem Kostenvoranschlag einverstanden, soll er direkt auf der Homepage einen Terminvorschlag zur Reparatur angeben können. Schickt der Kunde den Terminwunsch ab, sollen Terminwunsch, Kundennummer, Modell, Kennzeichen und die Liste an benötigten Reparaturen direkt an das Verwaltungssystem geschickt werden. Dort wird es dann bestätigt und an den Kunden verschickt.
3. Eine Authentifizierung soll nicht erfolgen.

Das Verwaltungssystem ist im internen Netz des Kunden per REST Call erreichbar und die Schnittstellenspezifikation wurde uns als OpenAPI zur Verfügung gestellt. Auch die Preisliste haben wir bereits erhalten und beide Dateien sind im Projekt als Ressourcen abgelegt.

# Aufgabe

Zwei Junior-Entwickler haben die Software für den Kunden entwickelt. Sie ist fast fertig. Was fehlt, um sie dem Kunden auszuliefern?

## Hinweise zur Bearbeitung

- Bitte bearbeite die Aufgabe ohne KI-Werkzeuge.
- Plane etwa zwei Stunden ein.
- Setze nur die Änderungen um, die für die Auslieferung unbedingt notwendig sind. Reicht die Zeit dafür nicht, genügt auch hier ein Kommentar, der beschreibt, was zu tun wäre.
- Was dir darüber hinaus auffällt, kannst du gerne als Kommentar an der passenden Stelle im Code hinterlassen.

## Abgabe

Bitte arbeite in einem **privaten Duplikat** dieses Repositorys, nicht in einem Fork. Forks öffentlicher Repositories lassen sich auf GitHub nicht auf privat stellen.

1. Lege in deinem GitHub-Account ein neues, leeres, **privates** Repository an, ohne README, `.gitignore` oder Lizenz.
2. Übertrage dieses Repository samt Historie dorthin:

   ```bash
   git clone --bare https://github.com/dicosgmbh/dcs-repiar-service.git
   cd dcs-repiar-service.git
   git push --mirror https://github.com/<DEIN-ACCOUNT>/<DEIN-REPOSITORY>.git
   cd ..
   rm -rf dcs-repiar-service.git
   ```

3. Klone dein privates Repository und committe deine Änderungen dort.
4. Lade unter _Settings → Collaborators_ diese GitHub-Accounts ein: `bhochberger`, `jtibke`.
5. Bitte stelle keinen Pull-Request auf dieses Repository.
