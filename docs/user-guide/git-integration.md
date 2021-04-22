# Git Integration

To enable Continuous Deployment & Delivery the Operator needs to be notified of `website.yaml` changes as well as
changes in content git repositories.

## Register Git Webhook

Registering your git repo's webhook makes any changes to your git repo immediately applied in your deployment and also 
your website's content.

Navigate to your git settings and add webhook.

* URL: `https://<operator-url>/api/webhook`
* TOKEN: the token defined in `my-website.yaml`

## Webhook Workflow

Once webhook is triggered following steps are performed:

1. Based on Git URL + TOKEN combination the operator finds appropriate website
2. Pull's the main repository and check if `website.yaml` file has changed. If yes then performs new rollout.
3. If no `website.yaml` change happens all components with same git url as webhook is refreshed. 

## Securing Webhook

Operator exposes `api/webhook` endpoint to all websites. To secure your website use unique security token in `spec.secretToken` 
to avoid unnecessary deployment/content refresh.

## Merge Request Previews

On Merge request event the operator creates a copy of forked website with website name suffixed by `-pr-<pr_number>`.
The copied website is deployed in exactly same way as source website - same included/excluded environments, secret token etc.

The only difference is that `git url` and `branch` is used from the Merge Request and display name is suffixed by ` - Fork`.

Once request is closed the forked website is deleted.

Deployment descriptor can control if preview environments are created or not via `previews` field.
