# SPAship Operator Installation Manifests

## Configuration

* `config/k8s.yaml` - Kubernetes configuration
* `openshift.yaml` - Openshift configuration

## Normal installation

* `install.yaml` - Standard installation with cluster access
* `install-ns.yaml` - Namespaced installation (no ClusterRole needed)
* `install-ns-nocrd.yaml` - Namespaced installation without CRD. Only One Tenant configuration enabled
