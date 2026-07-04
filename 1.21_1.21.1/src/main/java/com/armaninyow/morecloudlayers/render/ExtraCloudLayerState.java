package com.armaninyow.morecloudlayers.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.CloudStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Per-layer mirror of vanilla LevelRenderer's own cloud caching fields
 * (cloudBuffer / prevCloudX / prevCloudY / prevCloudZ / prevCloudColor / prevCloudsType),
 * plus a "dirty" flag we set externally on the three lifecycle triggers to force
 * a rebuild even when the camera hasn't moved (needed for Random mode to re-roll).
 */
public class ExtraCloudLayerState {
	public VertexBuffer buffer;
	public int prevX = Integer.MIN_VALUE;
	public int prevY = Integer.MIN_VALUE;
	public int prevZ = Integer.MIN_VALUE;
	public Vec3 prevColor;
	public CloudStatus prevType;
	public boolean dirty = true;

	public void close() {
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
	}
}