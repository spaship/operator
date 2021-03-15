# Custom CA Certificate

SSL Verification can be easily turned off by using `sslVerify` attribute in Website definition.

However, it's recommended to correctly setup CA certificates if they're not as part of default certificates.

To use custom CA certificate it's needed to customize 3 images:

1. Operator
2. Content Init Container
3. Content Api Container

## Build Own Images

One option is to build own images on top of Website CD default images.

Follow Docker files bellow. Each contains instructions how to build own image.
It's recommended to use version suffix e.g. `1.1.2-rhitca` to make clear what is the parent image or use same
version but different context.

1. [Operator](https://github.com/websitecd/operator/tree/main/manifests/customca/Dockerfile.operator)
2. [Content Init Container](https://github.com/websitecd/operator/tree/main/manifests/customca/Dockerfile.gitinit)
3. [Content Api Container](https://github.com/websitecd/content-git/blob/main/api/src/main/docker/Dockerfile.multistage)

It's possible to use Openshift build templates stored [next to Docker files](https://github.com/websitecd/operator/tree/main/manifests/customca)

Once such images are build the operator needs to use them.
Just set operator's system variables `APP_OPERATOR_IMAGE_INIT_VERSION` and `APP_OPERATOR_IMAGE_API_VERSION` in `websitecd-operator-config` config map.
