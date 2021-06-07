# REST API

The operator provides REST API. It's documented by Open API standard and available under
`http://<operator>/openapi` or actual copy is in [manifests/api](https://github.com/spaship/operator/tree/main/manifests/api) directory.

How API is secured is described in [next page](security.md).

## API List

The `/api` returns actual list of APIs.

```json
[
  "/api/webhook",
  "/api/v1/website/search?namespace={namespace}&name={name}",
  "/api/v1/website/{namespace}/{website}/{env}/applications",
  "/api/v1/website/{namespace}/{website}/{env}/applications/{name}"
]
```

## Webhook API

Under `/api/webhook` is exposed Webhook API to consume Git webhook. 

## Website API

Website API provides information about deployed websites.
Used by SPAship Manager UI.
Required roles: `spaship-user`

* `/api/v1/website/search?namespace={namespace}&name={name}`
  - List of websites managed by Operator.
  - Parameters `namespace` and `name` are optional.
* `/api/v1/website/{namespace}/{website}/{env}/applications`
  - List of Applications / Components
  - Path parameters `namespace`, `website`, `env` are required
* `/api/v1/website/{namespace}/{website}/{env}/applications/{name}`
  - Website application (component) detail - get actual data via Content API
  - Path parameters `namespace`, `website`, `env`, `name` are required
