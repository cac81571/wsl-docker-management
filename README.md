# WSL Docker Management

Windows 上から、WSL ディストリ内の Docker エンジンを操作するデスクトップ GUI です。  
Java Swing + FlatLaf で実装し、内部では `wsl -d <distro> -- docker ...` を呼び出します。

## 必要なもの

- Windows 10 / 11
- WSL 2（例: Ubuntu）
- WSL 内の Docker Engine（`docker` / `docker compose`）
- JDK 21 以上
- Maven 3.9+（初回ビルド時）

## 起動

### いちばん簡単

```bat
run.bat
```

- JAR がなければ自動で `mvn -q -DskipTests package` を実行します
- 起動後はコンソールを残しません（`javaw`）

### 手動

```bat
mvn -q -DskipTests package
java -jar target\wsl-docker-management-1.0.0-shaded.jar
```

接続確認だけする場合:

```bat
java -jar target\wsl-docker-management-1.0.0-shaded.jar --check
```

## 主な機能

| タブ | できること |
|---|---|
| コンテナ | 一覧、起動 / 停止 / 再起動 / 削除、ログ追従、inspect、非対話 exec |
| イメージ | 一覧、pull、エクスポート（`docker save`）、インポート（`load` / `import`）、inspect、削除 |
| ボリューム | 一覧、作成、inspect、削除 |
| ネットワーク | 一覧、作成、inspect、削除（`bridge` / `host` / `none` は保護） |
| Compose | 複数プロジェクト管理、`up -d` / `start` / `stop` / `restart` / `down` / `ps` / `logs` |

その他:

- WSL ディストリの選択・接続
- Docker デーモン起動の試行
- ライト / ダークテーマ切替
- F5 で現在タブを更新
- 削除などの破壊操作は確認ダイアログあり

## Compose タブの使い方

1. **追加...** または **参照して追加** で compose ファイルを登録
2. または **検出して追加** で `docker compose ls` の結果を取り込む
3. 一覧から選択して操作

- Windows パス（`C:\...`）と WSL パス（`/home/...`）の両方に対応
- 登録内容は再起動後も保持されます

## ビルド・テスト

```bat
mvn test
mvn -DskipTests package
mvn exec:java
```

成果物:

```text
target\wsl-docker-management-1.0.0-shaded.jar
```

## 設定ファイル

次の場所に保存されます。

```text
%USERPROFILE%\.wsl-docker-management\settings.properties
```

保存される内容の例:

- 最後に接続した WSL ディストリ
- テーマ（ダーク / ライト）
- 登録した Compose プロジェクト一覧

## 仕組み

1. `wsl --list --verbose` でディストリを列挙
2. 選択したディストリに対して `wsl -d <distro> -- docker ...` を実行
3. `--format json` の出力をパースして GUI に表示

Docker Desktop 専用の named pipe ではなく、**WSL 内の Docker CLI** を前提にしています。

## 注意

- Windows 側パス（`/mnt/c/...`）への大きなイメージの save / load は時間がかかることがあります
- `exec` は非対話（`sh -lc`）です。対話シェルや TTY は未対応です
- デーモン起動は `service docker start` / `systemctl start docker` を試します。sudo パスワードが必要な場合は失敗します

## ライセンス

このリポジトリにライセンスファイルがない場合は、利用前にリポジトリ所有者へ確認してください。
