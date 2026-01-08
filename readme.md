```yaml
- name: Create .env file
  run: |
      echo "SPRING_DATASOURCE_DATABASE=${{ secrets.DB_NAME }}" >> .env
      echo "SPRING_DATASOURCE_PASSWORD=${{ secrets.DB_PASS }}" >> .env
      echo "JASYPT_ENCRYPTOR_PASSWORD=${{ secrets.JASYPT_KEY }}" >> .env
```

OR
--
```yaml
- name: Build and Push Docker Image
  env:
    SPRING_DATASOURCE_PASSWORD: ${{ secrets.SPRING_DATASOURCE_PASSWORD }}
  run: |
    docker compose build
```

```shell
# Forgot to pull before push
git pull --rebase origin main
git push -U origin main
```