# kind node with what Longhorn needs on a node (iscsiadm, the NFS client for RWX volumes), see hack/kind/up.sh
ARG NODE_IMAGE
FROM ${NODE_IMAGE}
RUN apt-get update \
    && apt-get install -y --no-install-recommends open-iscsi nfs-common \
    && rm -rf /var/lib/apt/lists/* \
    && systemctl enable iscsid
