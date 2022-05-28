package me.ichun.mods.ichunutil.api.common.head.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.ichunutil.api.common.head.HeadInfo;
import net.minecraft.world.entity.animal.Parrot;


public class HeadParrot extends HeadInfo<Parrot>
{
    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    @Override
    public float getIrisScale(Parrot living, PoseStack stack, float partialTick, int eye)
    {
        return super.getIrisScale(living, stack, partialTick, eye) * (living.isPartyParrot() ? 1.6F : 1F);
    }
}
